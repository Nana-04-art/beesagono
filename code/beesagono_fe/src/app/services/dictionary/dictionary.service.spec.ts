import { TestBed } from '@angular/core/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { DictionaryService } from './dictionary.service';

describe('DictionaryService', () => {
  let service: DictionaryService;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [DictionaryService],
    });
    service = TestBed.inject(DictionaryService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should throw an error when getWordSet() is called before loadDictionary()', () => {
    expect(() => service.getWordSet()).toThrowError(
      'DictionaryService.getWordSet() chiamato prima del completamento di loadDictionary().'
    );
  });

  it('should load dictionary, sanitize, uppercase, and deduplicate an array-based JSON response', async () => {
    const rawMockData = [' miele ', 'Miele', 'APE', '123', 'bee-hive', '  vespa  '];

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify(rawMockData), { status: 200 })
    );

    const result = await service.loadDictionary();

    expect(result).toEqual(['MIELE', 'APE', 'VESPA']);

    const wordSet = service.getWordSet();
    expect(wordSet.size).toBe(3);
    expect(wordSet.has('MIELE')).toBe(true);
    expect(wordSet.has('APE')).toBe(true);
    expect(wordSet.has('VESPA')).toBe(true);
    expect(wordSet.has('123')).toBe(false);
  });

  it('should load dictionary when JSON response is an object with "words" property', async () => {
    const rawMockData = { words: ['casa', 'CASA', 'albero'] };

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify(rawMockData), { status: 200 })
    );

    const result = await service.loadDictionary();

    expect(result).toEqual(['CASA', 'ALBERO']);
    expect(service.getWordSet().has('ALBERO')).toBe(true);
  });

  it('should cache fetch calls and return cached list on subsequent loadDictionary() calls', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify(['MIELE']), { status: 200 })
    );

    const firstCall = await service.loadDictionary();
    const secondCall = await service.loadDictionary();

    expect(firstCall).toBe(secondCall);
    expect(fetchSpy).toHaveBeenCalledTimes(1);
  });

  it('should throw error on HTTP error response', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(null, { status: 404 })
    );

    await expect(service.loadDictionary()).rejects.toThrowError(
      'Impossibile caricare il dizionario (HTTP 404)'
    );
  });

  it('should throw error on invalid JSON shape (neither array nor object with words)', async () => {
    const invalidJson = { data: ['MIELE'] };

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify(invalidJson), { status: 200 })
    );

    await expect(service.loadDictionary()).rejects.toThrowError(
      'Il formato del dizionario non è valido (deve essere un array o un oggetto con chiave "words").'
    );
  });

  it('should throw error if JSON contains no valid alphabetic words after sanitization', async () => {
    const invalidData = ['123', '---', '  '];

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify(invalidData), { status: 200 })
    );

    await expect(service.loadDictionary()).rejects.toThrowError(
      'Il file dictionary.json non contiene parole valide.'
    );
  });
});