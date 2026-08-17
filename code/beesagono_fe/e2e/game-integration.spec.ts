import { test, expect } from '@playwright/test';

test.describe('Daily Game - Complete Integration & Player Flow', () => {

    test('covers initial visit, tile hits, word submission, stats update, mobile viewport, and persistence', async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 667 });

        await page.goto('/');

        const dismissNoticeBtn = page.getByRole('button', { name: 'Ho capito, fammi giocare!' });
        await dismissNoticeBtn.click();

        const centerTile = page.getByRole('button', { name: 'Lettera P (centrale e obbligatoria)' });
        await centerTile.click();

        await page.keyboard.type('ERO');
        await page.getByRole('button', { name: 'Invia la parola inserita' }).click();

        const rankTrigger = page.locator('#scoreboard-trigger');
        await rankTrigger.click();

        const scoreValue = page.locator('.score-value');
        await expect(scoreValue).not.toHaveText('0');

        await page.reload();

        await rankTrigger.click();
        await expect(scoreValue).not.toHaveText('0');
    });

});