import { TestBed } from '@angular/core/testing';

import { PuzzleGeneratorService } from './puzzle-generator.service';

describe('PuzzleGeneratorService', () => {
  let service: PuzzleGeneratorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PuzzleGeneratorService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
