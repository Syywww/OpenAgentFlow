import { expect, test } from '@playwright/test';

test('登录页提供完整认证入口', async ({ page }) => {
  await page.route('**/auth/captcha', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { captchaKey: 'e2e', imageBase64: '' } }),
    });
  });
  await page.goto('/login');

  await expect(page.getByRole('heading', { name: '登录控制台' })).toBeVisible();
  await expect(page.getByLabel('用户名')).toBeVisible();
  await expect(page.getByLabel('密码')).toBeVisible();
  await expect(page.getByText('验证码', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '登录' })).toBeVisible();
});
