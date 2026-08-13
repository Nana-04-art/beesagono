import { TestBed } from '@angular/core/testing';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { WelcomeNoticeService } from './welcome-notice.service';

describe('WelcomeNoticeService', () => {
  let service: WelcomeNoticeService;
  const STORAGE_KEY = 'mielegrammi_welcome_disclaimer_seen';

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [WelcomeNoticeService],
    });

    service = TestBed.inject(WelcomeNoticeService);
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should open notice if disclaimer has not been seen before', () => {
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
    expect(service.isNoticeOpen()).toBe(false);

    service.checkAndShowNotice();

    expect(service.isNoticeOpen()).toBe(true);
  });

  it('should not open notice if disclaimer has already been seen', () => {
    localStorage.setItem(STORAGE_KEY, 'true');
    expect(service.isNoticeOpen()).toBe(false);

    service.checkAndShowNotice();

    expect(service.isNoticeOpen()).toBe(false);
  });

  it('should dismiss notice, set localStorage flag, and close notice state', () => {
    service.checkAndShowNotice();
    expect(service.isNoticeOpen()).toBe(true);

    service.dismissNotice();

    expect(localStorage.getItem(STORAGE_KEY)).toBe('true');
    expect(service.isNoticeOpen()).toBe(false);
  });
});