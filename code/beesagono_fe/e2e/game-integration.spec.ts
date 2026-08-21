import { test, expect } from '@playwright/test';

test.describe('Daily Game - Complete Player Journey & Integration Flow', () => {

    test('covers onboarding, error handling, valid submission, mielegramma, stats, scoreboard, and persistence', async ({ page }) => {

        // 1. FIRST ACCESS (Mobile Viewport & Onboarding Modal)
        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        const dismissNoticeBtn = page.getByRole('button', { name: 'Ho capito, fammi giocare!' });
        await expect(dismissNoticeBtn).toBeVisible();
        await dismissNoticeBtn.click();
        await expect(dismissNoticeBtn).not.toBeVisible();

        // 2. INTERFACE & CENTER TILE VERIFICATION
        const centerTile = page.getByRole('button', { name: /centrale e obbligatoria/i });
        await expect(centerTile).toBeVisible();

        const centerLetter = (await centerTile.textContent())?.trim().toUpperCase() ?? '';
        expect(centerLetter).not.toBe('');

        const inputDisplay = page.locator('.word-display-container').first();
        const deleteBtn = page.locator('button:has-text("Cancella"), button:has-text("Elimina"), button[aria-label*="ancella"]').first();
        const shuffleBtn = page.locator('button:has-text("Mescola"), button[aria-label*="escola"]').first();
        const submitBtn = page.getByRole('button', { name: 'Invia la parola inserita' });

        await expect(deleteBtn).toBeVisible();
        await expect(submitBtn).toBeVisible();

        // Interaction with the Shuffle button
        if (await shuffleBtn.isVisible()) {
            await shuffleBtn.click();
            await expect(centerTile).toBeVisible();
        }

        // 3. ERROR HANDLING & IMMEDIATE FEEDBACK (UX Point 1)
        // Submitting an invalid/short word to trigger the error feedback
        await page.keyboard.type('NO');
        await submitBtn.click();

        // Verify the appearance of the error message inside the WordDisplay component
        const feedbackBadge = page.locator('.word-display-container .feedback-badge.text-danger');
        await expect(feedbackBadge).toBeVisible();

        // Clear input
        await deleteBtn.click();

        // 4. HIVE TILES EXTRACTION & VALID WORD / MIELEGRAMMA CALCULATION (UX Point 2)
        const dictResponse = await page.request.get('/dictionary.json');
        expect(dictResponse.ok()).toBeTruthy();

        const dictData = await dictResponse.json();
        const dictionary: string[] = (Array.isArray(dictData) ? dictData : (dictData.words || []))
            .map((w: string) => typeof w === 'string' ? w.trim().toUpperCase() : '')
            .filter((w: string) => w.length >= 4);

        let wordToSubmit = '';
        let isMielegramma = false;

        await expect.poll(async () => {
            const availableLetters = await page.evaluate(() => {
                const elements = Array.from(document.querySelectorAll('button, .tile, [class*="hex"]'));
                const letters: string[] = [];

                elements.forEach(el => {
                    const txt = (el.textContent || '').trim();
                    const aria = (el.getAttribute('aria-label') || '').toLowerCase();

                    const isSystemBtn = ['invia', 'cancella', 'elimina', 'mescola', 'ho capito', 'grado'].some(
                        cmd => txt.toLowerCase().includes(cmd) || aria.includes(cmd)
                    );

                    if (!isSystemBtn && txt.length === 1 && /^[A-Z]$/i.test(txt)) {
                        letters.push(txt.toUpperCase());
                    }
                });

                return [...new Set(letters)];
            });

            if (availableLetters.length === 0) return null;

            // Look for a Mielegramma first (7 unique letters using all tiles)
            const mielegrammaMatch = dictionary.find(word => {
                if (!word.includes(centerLetter)) return false;
                const uniqueChars = [...new Set(word.split(''))];
                return uniqueChars.length === 7 && word.split('').every(char => availableLetters.includes(char));
            });

            if (mielegrammaMatch) {
                wordToSubmit = mielegrammaMatch;
                isMielegramma = true;
                return mielegrammaMatch;
            }

            // Otherwise, fall back to a normal valid word
            const normalMatch = dictionary.find(word => {
                if (!word.includes(centerLetter)) return false;
                return word.split('').every(char => availableLetters.includes(char));
            });

            if (normalMatch) {
                wordToSubmit = normalMatch;
                return normalMatch;
            }

            return null;
        }, {
            message: 'Waiting to calculate a valid word from the hive tiles',
            timeout: 7000
        }).toBeTruthy();

        expect(wordToSubmit, 'Unable to find a valid word for the test').toBeTruthy();

        // 5. INPUT FOCUS & TYPING
        if (await inputDisplay.isVisible()) {
            await inputDisplay.click();
        } else {
            await page.locator('body').click({ position: { x: 10, y: 10 } });
        }

        await page.keyboard.type(wordToSubmit);

        const letterPattern = wordToSubmit.split('').join('\\s*');
        await expect(inputDisplay).toHaveText(new RegExp(letterPattern, 'i'));

        // Safely and reliably submit the word
        await submitBtn.waitFor({ state: 'visible' });
        await page.waitForTimeout(100); // Short breathing room for the UI
        await submitBtn.click({ force: true });

        // Reset display (the word is accepted and the input resets)
        await expect(inputDisplay).toContainText(/Digita o clicca le lettere/i);

        // 6. VERIFY WORD IN THE ACCORDION (.found-words-card)
        const foundWordsCard = page.locator('.found-words-card');
        await expect(foundWordsCard).toBeVisible();

        const toggleWordsBtn = foundWordsCard.locator('.toggle-button');
        await toggleWordsBtn.click();

        const wordChip = foundWordsCard.locator('.word-chip', { hasText: wordToSubmit });
        await expect(wordChip).toBeVisible();

        // Close the accordion again
        await toggleWordsBtn.click();

        // 7. STATS PANEL CONSULTATION (UX Point 3)
        await page.evaluate(() => window.scrollTo(0, 0));

        // The stats button in the navbar
        const statsBtn = page.locator('button[title*="statistiche"], .stats-popover-wrapper button').first();
        await expect(statsBtn).toBeVisible();
        await statsBtn.click();

        const statsPopover = page.locator('app-stats, .stats-popover-wrapper > div').first();
        await expect(statsPopover).toBeVisible();

        // Close the stats popover by clicking the button again
        await statsBtn.click();
        await expect(statsPopover).not.toBeVisible();

        // 8. OPENING MOBILE POPOVER/SCOREBOARD
        const rankTriggerMobile = page.locator('#scoreboard-trigger-mobile');
        await expect(rankTriggerMobile).toBeVisible();
        await rankTriggerMobile.click();

        const popoverMobile = page.locator('#scoreboard-popover-mobile');
        await expect(popoverMobile).toBeVisible();

        // Extract and verify the score and progress bar
        const scoreElement = popoverMobile.locator('.score-value');
        await expect(scoreElement).toBeVisible();

        const progressBar = popoverMobile.locator('.scoreboard-bar');
        await expect(progressBar).toBeVisible();

        const scoreText = (await scoreElement.textContent())?.trim() ?? '0';
        const scoreNum = parseInt(scoreText.replace(/\D/g, '') || '0', 10);
        expect(scoreNum).toBeGreaterThan(0);

        const initialRankText = (await rankTriggerMobile.textContent())?.trim();

        await rankTriggerMobile.click();
        await expect(popoverMobile).not.toBeVisible();

        // 9. PERSISTENCE UPON RELOAD
        await page.reload();

        await expect(dismissNoticeBtn).not.toBeVisible();

        // Reopen and verify data after reload
        await page.evaluate(() => window.scrollTo(0, 0));
        await expect(rankTriggerMobile).toBeVisible();
        await rankTriggerMobile.click();

        await expect(popoverMobile).toBeVisible();

        const reloadedScoreText = (await popoverMobile.locator('.score-value').textContent())?.trim();
        const reloadedRankText = (await rankTriggerMobile.textContent())?.trim();

        expect(reloadedScoreText).toBe(scoreText);
        expect(reloadedRankText).toBe(initialRankText);
    });

});