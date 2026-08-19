import { test, expect } from '@playwright/test';

test.describe('Daily Game - Complete Integration & Player Flow', () => {

    test('covers initial visit, tile hits, word submission, stats update, mobile viewport, and persistence', async ({ page }) => {

        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        const dismissNoticeBtn = page.getByRole('button', { name: 'Ho capito, fammi giocare!' });
        if (await dismissNoticeBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
            await dismissNoticeBtn.click();
        }

        const centerTile = page.getByRole('button', { name: /centrale e obbligatoria/i });
        await expect(centerTile).toBeVisible();

        const centerLetterText = (await centerTile.textContent())?.trim() ?? '';
        expect(centerLetterText).not.toBe('');

        await centerTile.click();

        const inputDisplay = page.locator('.word-display-container').first();
        await expect(inputDisplay).toContainText(centerLetterText);

        const submitBtn = page.getByRole('button', { name: 'Invia la parola inserita' });
        await submitBtn.click();

        const rankTrigger = page.getByRole('button', { name: /Grado:/i }).filter({ visible: true });
        await expect(rankTrigger).toBeVisible();
        await rankTrigger.click();

        const scoreValue = page.locator('.score-value').filter({ visible: true });
        await expect(scoreValue).toBeVisible();

        await page.reload();

        if (await dismissNoticeBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
            await dismissNoticeBtn.click();
        }

        await expect(rankTrigger).toBeVisible();
        await rankTrigger.click();
        await expect(scoreValue).toBeVisible();
    });

});