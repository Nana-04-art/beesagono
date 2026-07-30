import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HoneycombGridComponent } from './honeycomb-grid.component';

describe('HoneycombGridComponent', () => {
  let component: HoneycombGridComponent;
  let fixture: ComponentFixture<HoneycombGridComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HoneycombGridComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HoneycombGridComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
