import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { DictionaryService } from './dictionary.service';
import { WordValidationRequest, WordValidationResponse } from '../../models/game/word-validation.model';
import { environment } from '../../environments/environment';

describe('DictionaryService', () => {
  let service: DictionaryService;
  let httpMock: HttpTestingController;

  const mockBaseUrl = `${environment.apiBaseUrl}/dictionary`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        DictionaryService
      ],
    });

    service = TestBed.inject(DictionaryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('validateWord', () => {
    it('should send a POST request with sanitized payload and return validation response', () => {
      const inputWord = '  miele  ';
      const mockResponse: WordValidationResponse = {
        word: 'MIELE',
        valid: true,
        pointsEarned: 5,
        isMielegramma: false,
        errorCode: '',
        errorMessage: ''
      };

      service.validateWord(inputWord).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${mockBaseUrl}/validate`);
      expect(req.request.method).toBe('POST');

      const expectedPayload: WordValidationRequest = {
        word: 'MIELE'
      };
      expect(req.request.body).toEqual(expectedPayload);

      req.flush(mockResponse);
    });

    it('should propagate error when HTTP request fails', () => {
      const word = 'APE';

      service.validateWord(word).subscribe({
        next: () => expect.fail('Should have failed with an error'),
        error: (error) => {
          expect(error.status).toBe(500);
        }
      });

      const req = httpMock.expectOne(`${mockBaseUrl}/validate`);
      req.flush('Internal Server Error', { status: 500, statusText: 'Server Error' });
    });
  });
});