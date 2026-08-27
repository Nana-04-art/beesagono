import { test, expect } from '@playwright/test';

const GAME_RULES = {
    MIN_WORD_LENGTH: 4,
    MIELEGRAMMA_BONUS: 7,
} as const;

test.describe('Daily Game - Complete Player Journey & Application Coverage', () => {

    test('1. Covers onboarding, error handling, mielegramma submission, stats, scoreboard, and persistence', async ({ page }) => {

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
        await expect(shuffleBtn).toBeVisible();
        await shuffleBtn.click();
        await expect(centerTile).toBeVisible();

        // 3. ERROR HANDLING & IMMEDIATE FEEDBACK
        await inputDisplay.click();
        await page.keyboard.type('NO');
        await expect(inputDisplay).toContainText(/N\s*O/i);

        await submitBtn.click();

        const feedbackBadge = page.locator('.word-display-container .feedback-badge.text-danger');
        await expect(feedbackBadge).toBeVisible();
        await expect(feedbackBadge).toContainText(/corta/i);

        // A rejected too-short submission clears currentInput automatically.
        await expect(inputDisplay).toContainText(/Digita o clicca le lettere/i);

        // 4. HIVE TILES EXTRACTION & MIELEGRAMMA CALCULATION
        const dictResponse = await page.request.get('/dictionary.json');
        expect(dictResponse.ok()).toBeTruthy();

        const dictData = await dictResponse.json();
        const dictionary: string[] = (Array.isArray(dictData) ? dictData : (dictData.words || []))
            .map((w: string) => typeof w === 'string' ? w.trim().toUpperCase() : '')
            .filter((w: string) => w.length >= GAME_RULES.MIN_WORD_LENGTH);

        let mielegrammaWord = '';

        await expect.poll(async () => {
            mielegrammaWord = await page.evaluate((dict) => {
                const elements = Array.from(document.querySelectorAll('button, .tile, [class*="hex"]'));
                const letters = new Set<string>();
                let center = '';

                elements.forEach(el => {
                    const txt = (el.textContent || '').trim().toUpperCase();
                    const aria = (el.getAttribute('aria-label') || '').toLowerCase();
                    const isSystem = ['invia', 'cancella', 'elimina', 'mescola', 'ho capito', 'grado'].some(
                        c => txt.toLowerCase().includes(c) || aria.includes(c)
                    );

                    if (!isSystem && txt.length === 1 && /^[A-Z]$/.test(txt)) {
                        letters.add(txt);
                        if (el.classList.contains('is-center') || aria.includes('centrale')) {
                            center = txt;
                        }
                    }
                });

                if (!center && letters.size > 0) {
                    center = Array.from(letters)[0];
                }

                const match = dict.find((w: string) => {
                    if (!center || !w.includes(center)) return false;
                    const unique = new Set(w);
                    if (unique.size !== 7) return false;
                    return w.split('').every((char: string) => letters.has(char));
                });

                return match || '';
            }, dictionary);

            return mielegrammaWord.length > 0 ? mielegrammaWord : null;
        }, {
            message: "Waiting to find today's Mielegramma among the hive tiles",
            timeout: 7000
        }).toBeTruthy();

        expect(mielegrammaWord).toBeTruthy();

        // 5. READ THE SCORE BEFORE SUBMISSION
        const rankTriggerMobile = page.locator('#scoreboard-trigger-mobile');
        const popoverMobile = page.locator('#scoreboard-popover-mobile');
        const scoreElement = popoverMobile.locator('.score-value');

        await expect(rankTriggerMobile).toBeVisible();
        await rankTriggerMobile.click();
        await expect(popoverMobile).toBeVisible();
        await expect(scoreElement).toHaveText('0');
        await rankTriggerMobile.click();
        await expect(popoverMobile).not.toBeVisible();

        // 6. INPUT FOCUS, TYPING & SUBMISSION OF THE MIELEGRAMMA
        await inputDisplay.click();
        await page.keyboard.type(mielegrammaWord);

        const letterPattern = mielegrammaWord.split('').join('\\s*');
        await expect(inputDisplay).toHaveText(new RegExp(letterPattern, 'i'));

        await expect(submitBtn).toBeEnabled();
        await submitBtn.click();

        // Faithful verification of success feedback using a new name for the constant
        const successFeedbackBadge = page.locator('.word-display-container .feedback-badge');
        await expect(successFeedbackBadge).toBeVisible();
        await expect(successFeedbackBadge).toContainText(/mielegramma/i);

        // 7. VERIFY WORD IN ACCORDION (.word-badge)
        const foundWordsCard = page.locator('.found-words-card');
        await expect(foundWordsCard).toBeVisible();

        const toggleWordsBtn = foundWordsCard.locator('.toggle-button');
        await toggleWordsBtn.click();

        const wordChip = foundWordsCard.locator('.word-badge', { hasText: mielegrammaWord });
        await expect(wordChip).toBeVisible();
        await expect(wordChip).toHaveClass(/is-mielegramma/);

        await toggleWordsBtn.click();

        // 8. STATS PANEL CONSULTATION
        await page.evaluate(() => window.scrollTo(0, 0));

        const statsBtn = page.locator('button[title*="statistiche"], .stats-popover-wrapper button').first();
        await expect(statsBtn).toBeVisible();
        await statsBtn.click();

        const statsPopover = page.locator('app-stats, .stats-popover-wrapper > div').first();
        await expect(statsPopover).toBeVisible();

        await statsBtn.click();
        await expect(statsPopover).not.toBeVisible();

        // 9. SCOREBOARD: VERIFY EXACT MIELEGRAMMA SCORE
        const expectedScore = mielegrammaWord.length + GAME_RULES.MIELEGRAMMA_BONUS;

        await expect(rankTriggerMobile).toBeVisible();
        await rankTriggerMobile.click();
        await expect(popoverMobile).toBeVisible();
        await expect(scoreElement).toHaveText(String(expectedScore));

        const progressBar = popoverMobile.locator('.scoreboard-bar');
        await expect(progressBar).toBeVisible();

        const initialRankText = (await rankTriggerMobile.textContent())?.trim();
        await rankTriggerMobile.click();
        await expect(popoverMobile).not.toBeVisible();

        // 10. PERSISTENCE CHECK ON LOCALSTORAGE & RELOAD
        await expect.poll(async () => {
            return await page.evaluate((expected) => {
                for (let i = 0; i < localStorage.length; i++) {
                    const key = localStorage.key(i);
                    if (key && key.includes('game:')) {
                        try {
                            const val = JSON.parse(localStorage.getItem(key) || '{}');
                            if (val && val.score === expected) return true;
                        } catch { }
                    }
                }
                return false;
            }, expectedScore);
        }).toBeTruthy();

        await page.reload();
        await expect(dismissNoticeBtn).not.toBeVisible();

        await page.evaluate(() => window.scrollTo(0, 0));
        await expect(rankTriggerMobile).toBeVisible();
        await rankTriggerMobile.click();
        await expect(popoverMobile).toBeVisible();

        const reloadedScoreText = (await popoverMobile.locator('.score-value').textContent())?.trim();
        const reloadedRankText = (await rankTriggerMobile.textContent())?.trim();

        expect(reloadedScoreText).toBe(String(expectedScore));
        expect(reloadedRankText).toBe(initialRankText);
    });

    test('2. Accepts a normal (non-pangram) valid word dynamically found and updates found words list', async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        // Secure and blocking closure of the welcome modal for the second test as well
        const dismissNoticeBtn = page.getByRole('button', { name: 'Ho capito, fammi giocare!' });
        await expect(dismissNoticeBtn).toBeVisible();
        await dismissNoticeBtn.click();
        await expect(dismissNoticeBtn).not.toBeVisible();

        const centerTile = page.getByRole('button', { name: /centrale e obbligatoria/i });
        await expect(centerTile).toBeVisible();
        const centerLetter = (await centerTile.textContent())?.trim().toUpperCase() ?? '';

        const inputDisplay = page.locator('.word-display-container').first();
        const submitBtn = page.getByRole('button', { name: 'Invia la parola inserita' });

        const dictResponse = await page.request.get('/dictionary.json');
        const dictData = await dictResponse.json();
        const dictionary: string[] = (Array.isArray(dictData) ? dictData : (dictData.words || []))
            .map((w: string) => typeof w === 'string' ? w.trim().toUpperCase() : '')
            .filter((w: string) => w.length >= GAME_RULES.MIN_WORD_LENGTH);

        let normalWord = '';

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

            const match = dictionary.find(word => {
                if (!word.includes(centerLetter)) return false;
                const uniqueChars = new Set(word.split(''));
                if (uniqueChars.size === 7) return false;
                return word.split('').every(char => availableLetters.includes(char));
            });

            normalWord = match ?? '';
            return match ?? null;
        }, {
            message: 'Waiting to find a normal (non-pangram) valid word among the hive tiles',
            timeout: 7000
        }).toBeTruthy();

        expect(normalWord).toBeTruthy();

        await inputDisplay.click();
        await page.keyboard.type(normalWord);

        const letterPattern = normalWord.split('').join('\\s*');
        await expect(inputDisplay).toHaveText(new RegExp(letterPattern, 'i'));

        await expect(submitBtn).toBeEnabled();
        await submitBtn.click();

        await expect(inputDisplay).toContainText(/Digita o clicca le lettere/i);

        const foundWordsCard = page.locator('.found-words-card');
        await foundWordsCard.locator('.toggle-button').click();
        await expect(foundWordsCard.locator('.word-badge', { hasText: normalWord })).toBeVisible();
    });

    test('3. Accepts input via SVG Honeycomb grid clicks and controls', async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        const dismissNoticeBtn = page.getByRole('button', { name: 'Ho capito, fammi giocare!' });
        await expect(dismissNoticeBtn).toBeVisible();
        await dismissNoticeBtn.click();

        const inputDisplay = page.locator('.word-display-container').first();

        const centerCell = page.locator('.hex-group[aria-label*="centrale"]').first();
        await expect(centerCell).toBeVisible();
        await centerCell.click();

        const outerCell = page.locator('.hex-group:not([aria-label*="centrale"])').first();
        await outerCell.click();

        await expect(inputDisplay).not.toContainText(/Digita o clicca le lettere/i);

        const deleteBtn = page.locator('button:has-text("Cancella"), button:has-text("Elimina"), button[aria-label*="ancella"]').first();
        await deleteBtn.click();
        await deleteBtn.click();

        await expect(inputDisplay).toContainText(/Digita o clicca le lettere/i);
    });

    test('4. Records invalid attempts and renders them in the invalid-words component', async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        const dismissNoticeBtn = page.getByRole('button', { name: 'Ho capito, fammi giocare!' });
        await expect(dismissNoticeBtn).toBeVisible();
        await dismissNoticeBtn.click();

        const inputDisplay = page.locator('.word-display-container').first();
        const submitBtn = page.getByRole('button', { name: 'Invia la parola inserita' });

        await inputDisplay.click();
        await page.keyboard.type('ZZZZ');
        await submitBtn.click();

        const invalidCard = page.locator('.invalid-words-card');
        await expect(invalidCard).toBeVisible();

        const toggleBtn = invalidCard.locator('.toggle-button');
        const isExpanded = await toggleBtn.getAttribute('aria-expanded');
        if (isExpanded === 'false') {
            await toggleBtn.click();
        }

        const invalidChip = invalidCard.locator('.invalid-chip', { hasText: 'ZZZZ' });
        await expect(invalidChip).toBeVisible();
    });

    test('5. Toggles word map view inside found-words component', async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        const dismissNoticeBtn = page.getByRole('button', { name: 'Ho capito, fammi giocare!' });
        await expect(dismissNoticeBtn).toBeVisible();
        await dismissNoticeBtn.click();

        const foundWordsCard = page.locator('.found-words-card');
        const accordionToggle = foundWordsCard.locator('.toggle-button');
        await accordionToggle.click();

        const mapToggleBtn = foundWordsCard.locator('button[aria-label*="mappa"], button[aria-label*="parole"]').last();
        await expect(mapToggleBtn).toBeVisible();
        await mapToggleBtn.click();

        const wordMap = foundWordsCard.locator('app-word-map');
        await expect(wordMap).toBeVisible();
    });

});