import { TestBed } from '@angular/core/testing';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { WelcomeNoticeService } from './welcome-notice.service';
import { StorageService } from '../storage/storage.service';

describe('WelcomeNoticeService', () => {
  let service: WelcomeNoticeService;
  let mockStorageService: Partial<StorageService>;

  beforeEach(() => {
    mockStorageService = {
      load: vi.fn(),
      save: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        WelcomeNoticeService,
        { provide: StorageService, useValue: mockStorageService },
      ],
    });

    service = TestBed.inject(WelcomeNoticeService);
  });

  it('should show notice if key is not present in storage', () => {
    vi.spyOn(mockStorageService, 'load').mockReturnValue(null);
    service.checkAndShowNotice();
    expect(service.isNoticeOpen()).toBe(true);
  });

  it('should not show notice if key is present in storage', () => {
    vi.spyOn(mockStorageService, 'load').mockReturnValue(true);
    service.checkAndShowNotice();
    expect(service.isNoticeOpen()).toBe(false);
  });

  it('should safely open notice when storage throws a SecurityError', () => {
    vi.spyOn(mockStorageService, 'load').mockImplementation(() => {
      throw new DOMException('Access denied', 'SecurityError');
    });

    service.checkAndShowNotice();
    expect(service.isNoticeOpen()).toBe(true);
  });

  it('should dismiss notice and safely handle save exceptions', () => {
    service.isNoticeOpen.set(true);
    vi.spyOn(mockStorageService, 'save').mockImplementation(() => {
      throw new DOMException('Quota exceeded', 'QuotaExceededError');
    });

    service.dismissNotice();
    expect(service.isNoticeOpen()).toBe(false);
  });
});