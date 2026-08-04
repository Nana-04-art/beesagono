import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HoneycombGridComponent } from './honeycomb-grid.component';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { Cell } from '../../models/cell.model';

describe('HoneycombGridComponent', () => {
  let component: HoneycombGridComponent;
  let fixture: ComponentFixture<HoneycombGridComponent>;

  const mockCells: Cell[] = [
    { id: '1', letter: 'A', isCenter: true, position: 0 },
    { id: '2', letter: 'B', isCenter: false, position: 1 },
    { id: '3', letter: 'C', isCenter: false, position: 2 },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HoneycombGridComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HoneycombGridComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('cells', mockCells);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render all cells provided in input', () => {
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('A');
    expect(element.textContent).toContain('B');
    expect(element.textContent).toContain('C');
  });

  it('should emit letterTapped event when a cell is clicked', () => {
    const spy = vi.spyOn(component.letterTapped, 'emit');

    // Chiamata diretta al metodo scatenato dal click o simula l'interazione
    component.onCellClick('A');

    expect(spy).toHaveBeenCalledWith('A');
  });

  it('should emit letterTapped on Enter or Space keydown', () => {
    const spy = vi.spyOn(component.letterTapped, 'emit');

    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
    component.onKeyDown(enterEvent, 'B');

    expect(spy).toHaveBeenCalledWith('B');
  });
});