import { WritableSignal } from '@angular/core';

/**
 * Returns the current date or the passed date in YYYY-MM-DD format
 */
export function getTodayIsoString(date = new Date()): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

export function deleteLastChar(inputSignal: WritableSignal<string>): void {
    inputSignal.update((prev) => prev.slice(0, -1));
}

export function clearInput(inputSignal: WritableSignal<string>): void {
    inputSignal.set('');
}

export function shuffleOrder(shuffleOrderSignal: WritableSignal<number[]>): void {
    const currentOrder = [...shuffleOrderSignal()];
    for (let i = currentOrder.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [currentOrder[i], currentOrder[j]] = [currentOrder[j], currentOrder[i]];
    }
    shuffleOrderSignal.set(currentOrder);
}

export interface HandleInputParams<TReturn> {
    rawChar: string;
    isReady: boolean;
    currentInputSignal: WritableSignal<string>;
    submitFn: () => TReturn;
}

/**
 * Handles physical keyboard or on-screen keyboard input
 */
export function processInput<TReturn>({
    rawChar,
    isReady,
    currentInputSignal,
    submitFn,
}: HandleInputParams<TReturn>): TReturn | void {
    if (!isReady) return;

    const normalized = rawChar.toUpperCase();
    if (normalized === 'BACKSPACE') {
        deleteLastChar(currentInputSignal);
    } else if (normalized === 'ENTER') {
        return submitFn();
    } else if (/^[A-Z]$/.test(normalized)) {
        currentInputSignal.update((prev) => prev + normalized);
    }
}