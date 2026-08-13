import { TestBed } from '@angular/core/testing';

import { WelcomeNoticeService } from './welcome-notice.service';

describe('WelcomeNoticeService', () => {
  let service: WelcomeNoticeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WelcomeNoticeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
