import { expect, test } from '@playwright/test';

function apiResponse(data: unknown) {
  return JSON.stringify({
    success: true,
    code: 'SUCCESS',
    message: 'success',
    data,
    timestamp: '2026-08-03T00:00:00Z',
  });
}

test('登录页提供完整认证入口', async ({ page }) => {
  await page.route('**/auth/captcha', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: apiResponse({ captchaKey: 'e2e', imageBase64: '', expireSeconds: 120 }),
    });
  });
  await page.goto('/login');

  await expect(page.getByRole('heading', { name: '登录控制台' })).toBeVisible();
  await expect(page.getByLabel('用户名')).toBeVisible();
  await expect(page.getByLabel('密码')).toBeVisible();
  await expect(page.getByText('验证码', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '登录' })).toBeVisible();
});

test('登录后写入默认空间并按权限裁剪菜单', async ({ page }) => {
  // 未单独声明的页面请求返回空对象，避免后台辅助请求干扰认证主链路。
  await page.route('http://localhost:8080/api/**', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: apiResponse({}) });
  });
  await page.route('**/auth/captcha', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: apiResponse({ captchaKey: 'e2e-login', imageBase64: '', expireSeconds: 120 }),
    });
  });
  await page.route('**/auth/login', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: apiResponse({
        accessToken: 'e2e-access-token',
        tokenType: 'Bearer',
        expiresAt: '2026-08-03T01:00:00Z',
        currentUser: {
          id: 'user-e2e',
          username: 'operator',
          displayName: '运营人员',
          email: 'operator@example.com',
          roles: ['operator'],
          permissions: ['dashboard:view'],
        },
      }),
    });
  });
  await page.route('**/workspaces', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: apiResponse([
        { id: 'workspace-secondary', defaultFlag: false },
        { id: 'workspace-default', defaultFlag: true },
      ]),
    });
  });
  await page.route('**/dashboard/overview', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: apiResponse({
        agentCount: 0,
        publishedAgentCount: 0,
        knowledgeBaseCount: 0,
        toolCount: 0,
        enabledToolCount: 0,
        mcpServerCount: 0,
        workflowCount: 0,
        todayRunCount: 0,
        todaySuccessCount: 0,
        todayFailureCount: 0,
        todaySuccessRate: 0,
        todayCost: 0,
        todayTokenCount: 0,
        todayAvgLatencyMs: 0,
        taskBacklogCount: 0,
        openAlertCount: 0,
        unhealthyComponentCount: 0,
        knowledgeHealth: {
          documentCount: 0,
          parsedDocumentCount: 0,
          failedDocumentCount: 0,
          processingDocumentCount: 0,
          chunkCount: 0,
          embeddingCount: 0,
          openIssueCount: 0,
          highRiskIssueCount: 0,
          unsyncedEmbeddingCount: 0,
        },
        runTrend: [],
        recentRuns: [],
        modelUsage: [],
        taskQueue: [],
        openAlerts: [],
        healthChecks: [],
        insights: [],
      }),
    });
  });

  await page.goto('/login');
  await page.getByLabel('用户名').fill('operator');
  await page.getByLabel('密码').fill('123456');
  await page.getByPlaceholder('请输入验证码').fill('9527');
  await page.getByRole('button', { name: '登录' }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole('link', { name: '工作台', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: '系统设置', exact: true })).toHaveCount(0);
  await expect(page.getByRole('link', { name: '智能体', exact: true })).toHaveCount(0);

  const authState = await page.evaluate(() => ({
    token: localStorage.getItem('oaf_access_token'),
    workspaceId: localStorage.getItem('oaf_active_workspace_id'),
  }));
  expect(authState).toEqual({ token: 'e2e-access-token', workspaceId: 'workspace-default' });
});
