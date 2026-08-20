import { test, expect } from '@playwright/test';

test.describe('Daily Game - Complete Integration & Player Flow', () => {

    test('covers initial visit, tile hits, word submission, stats update, mobile viewport, and persistence', async ({ page }) => {

        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        const dismissNoticeBtn = page.getByRole('button', { name: 'Ho capito, fammi giocare!' });
        await expect(dismissNoticeBtn).toBeVisible();
        await dismissNoticeBtn.click();

        const centerTile = page.getByRole('button', { name: /centrale e obbligatoria/i });
        await expect(centerTile).toBeVisible();

        const centerLetter = (await centerTile.textContent())?.trim().toUpperCase() ?? '';
        expect(centerLetter).not.toBe('');

        const validWord = await page.evaluate(() => {
            const gameData = localStorage.getItem('beesagono_game_state');
            if (gameData) {
                const parsed = JSON.parse(gameData);
                if (parsed?.board?.words?.length > 0) {
                    return parsed.board.words[0];
                }
            }
            return null;
        });

        await centerTile.click();
        const inputDisplay = page.locator('.word-display-container').first();
        await expect(inputDisplay).toContainText(centerLetter);

        const deleteBtn = page.locator('button:has-text("Cancella"), button:has-text("Elimina"), button[aria-label*="ancella"], button[aria-label*="limina"]').first();
        await expect(deleteBtn).toBeVisible();
        await deleteBtn.click();
        await expect(inputDisplay).toContainText(/Digita o clicca le lettere/i);


        const wordToSubmit = validWord || (centerLetter + 'AAAA');
        await page.keyboard.type(wordToSubmit);

        const submitBtn = page.getByRole('button', { name: /Invia/i });
        await expect(submitBtn).toBeVisible();
        await submitBtn.click();

        const scoreValue = page.locator('.score-value, .current-score').first();
        const wordsCount = page.locator('.found-words-count, .words-count').first();

        if (await scoreValue.isVisible().catch(() => false)) {
            const scoreText = (await scoreValue.textContent())?.trim() ?? '0';
            const scoreNum = parseInt(scoreText, 10);
            expect(scoreNum).toBeGreaterThan(0);
        }

        const rankTrigger = page.getByRole('button', { name: /Grado:/i }).filter({ visible: true });
        await expect(rankTrigger).toBeVisible();
        const initialRankText = (await rankTrigger.textContent())?.trim();
        await page.reload();

        await expect(rankTrigger).toBeVisible();
        const reloadedRankText = (await rankTrigger.textContent())?.trim();
        expect(reloadedRankText).toBe(initialRankText);
    });

});