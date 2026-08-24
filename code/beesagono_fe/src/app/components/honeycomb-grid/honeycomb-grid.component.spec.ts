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

  it('should emit letterTapped and blur target on cell click', () => {
    const spy = vi.spyOn(component.letterTapped, 'emit');
    const blurSpy = vi.fn();

    const mockEvent = {
      currentTarget: { blur: blurSpy },
    } as unknown as MouseEvent;

    component.onCellClick(mockEvent, 'A');

    expect(blurSpy).toHaveBeenCalled();
    expect(spy).toHaveBeenCalledWith('A');
  });

  it('should emit letterTapped and prevent default on Enter or Space keydown', () => {
    const spy = vi.spyOn(component.letterTapped, 'emit');
    const blurSpy = vi.fn();

    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
    vi.spyOn(enterEvent, 'preventDefault');
    Object.defineProperty(enterEvent, 'currentTarget', {
      value: { blur: blurSpy },
    });

    component.onKeyDown(enterEvent, 'B');

    expect(enterEvent.preventDefault).toHaveBeenCalled();
    expect(blurSpy).toHaveBeenCalled();
    expect(spy).toHaveBeenCalledWith('B');

    const spaceEvent = new KeyboardEvent('keydown', { key: ' ' });
    vi.spyOn(spaceEvent, 'preventDefault');

    component.onKeyDown(spaceEvent, 'C');

    expect(spaceEvent.preventDefault).toHaveBeenCalled();
    expect(spy).toHaveBeenCalledWith('C');
  });

  it('should not emit letterTapped for unhandled keys', () => {
    const spy = vi.spyOn(component.letterTapped, 'emit');

    const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
    component.onKeyDown(escapeEvent, 'B');

    expect(spy).not.toHaveBeenCalled();
  });

  describe('getHexCoordinates', () => {
    it('should return center coordinates for position 0', () => {
      const coords = component.getHexCoordinates(0);
      expect(coords).toEqual({ x: 160, y: 160 });
    });

    it('should calculate outer radial coordinates correctly for position 1 (top-center)', () => {
      const coords = component.getHexCoordinates(1);
      expect(coords.x).toBeCloseTo(160, 5);
      expect(coords.y).toBeCloseTo(160 - 92, 5); // 68
    });

    it('should calculate distinct coordinates for other outer positions', () => {
      const pos1 = component.getHexCoordinates(1);
      const pos2 = component.getHexCoordinates(2);

      expect(pos1).not.toEqual(pos2);
      expect(pos2.x).toBeGreaterThan(160);
    });
  });
});