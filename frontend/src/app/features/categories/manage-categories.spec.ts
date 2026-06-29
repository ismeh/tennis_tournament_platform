import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ManageCategoriesComponent } from './manage-categories';
import { AuthService } from '../../core/auth/auth.service';
import { CustomCategoryService } from '../../data/services/custom-category.service';
import { TournamentEventCatalogItem } from '../../data/interfaces/tournament.model';

describe('ManageCategoriesComponent', () => {
  let component: ManageCategoriesComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let categoryServiceSpy: jasmine.SpyObj<CustomCategoryService>;

  const mockCategories: TournamentEventCatalogItem[] = [
    { id: 1, category: 'Senior A', description: '', custom: true },
    { id: 2, category: 'Junior B', description: '', custom: true },
  ];

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      currentRole: 'ORGANIZER',
    });

    categoryServiceSpy = jasmine.createSpyObj('CustomCategoryService', [
      'getMyCategories',
      'createCategory',
      'updateCategory',
      'deleteCategory',
    ]);

    categoryServiceSpy.getMyCategories.and.returnValue(of(mockCategories));
    categoryServiceSpy.createCategory.and.returnValue(
      of({ id: 3, category: 'New', description: '', custom: true })
    );
    categoryServiceSpy.updateCategory.and.returnValue(
      of({ id: 1, category: 'Updated', description: '', custom: true })
    );
    categoryServiceSpy.deleteCategory.and.returnValue(of(undefined));

    spyOn(window, 'confirm').and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [ManageCategoriesComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: CustomCategoryService, useValue: categoryServiceSpy },
        provideRouter([{ path: '**', component: ManageCategoriesComponent }]),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ManageCategoriesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('loadCategories', () => {
    it('should load categories on init', () => {
      expect(component.categories().length).toBe(2);
      expect(component.isLoading()).toBeFalse();
    });

    it('should handle load error', () => {
      categoryServiceSpy.getMyCategories.and.returnValue(throwError(() => new Error('fail')));
      component.loadCategories();
      expect(component.categories().length).toBe(0);
    });
  });

  describe('createCategory', () => {
    it('should create a new category', () => {
      component.newCategoryName = 'New Cat';
      component.createCategory();
      expect(categoryServiceSpy.createCategory).toHaveBeenCalledWith('New Cat');
      expect(component.categories().length).toBe(3);
      expect(component.successMessage()).toBeTruthy();
    });

    it('should not create if name is empty', () => {
      component.newCategoryName = '   ';
      component.createCategory();
      expect(categoryServiceSpy.createCategory).not.toHaveBeenCalled();
    });

    it('should handle create error', () => {
      categoryServiceSpy.createCategory.and.returnValue(
        throwError(() => ({ error: { message: 'Duplicate' } }))
      );
      component.newCategoryName = 'Dup';
      component.createCategory();
      expect(component.errorMessage()).toBe('Duplicate');
    });

    it('should handle create error without message', () => {
      categoryServiceSpy.createCategory.and.returnValue(
        throwError(() => ({}))
      );
      component.newCategoryName = 'Fail';
      component.createCategory();
      expect(component.errorMessage()).toBeTruthy();
    });
  });

  describe('edit', () => {
    it('should start edit mode', () => {
      component.startEdit(mockCategories[0]);
      expect(component.editingId()).toBe(1);
    });

    it('should cancel edit mode', () => {
      component.startEdit(mockCategories[0]);
      component.cancelEdit();
      expect(component.editingId()).toBeNull();
    });
  });

  describe('saveEdit', () => {
    it('should save edited category', () => {
      component.startEdit(mockCategories[0]);
      component.editingName = 'Updated Cat';
      component.saveEdit(mockCategories[0]);
      expect(categoryServiceSpy.updateCategory).toHaveBeenCalled();
      expect(component.editingId()).toBeNull();
    });

    it('should not save if name is empty', () => {
      component.startEdit(mockCategories[0]);
      component.editingName = '   ';
      component.saveEdit(mockCategories[0]);
      expect(categoryServiceSpy.updateCategory).not.toHaveBeenCalled();
    });

    it('should handle update error', () => {
      categoryServiceSpy.updateCategory.and.returnValue(
        throwError(() => ({ error: { message: 'Error' } }))
      );
      component.startEdit(mockCategories[0]);
      component.editingName = 'Fail';
      component.saveEdit(mockCategories[0]);
      expect(component.errorMessage()).toBe('Error');
    });

    it('should handle update error without message', () => {
      categoryServiceSpy.updateCategory.and.returnValue(throwError(() => ({})));
      component.startEdit(mockCategories[0]);
      component.editingName = 'Fail';
      component.saveEdit(mockCategories[0]);
      expect(component.errorMessage()).toBeTruthy();
    });
  });

  describe('confirmDelete', () => {
    it('should delete category after confirm', () => {
      component.confirmDelete(mockCategories[0]);
      expect(categoryServiceSpy.deleteCategory).toHaveBeenCalledWith(1);
      expect(component.categories().length).toBe(1);
    });

    it('should not delete if confirm is cancelled', () => {
      (window.confirm as jasmine.Spy).and.returnValue(false);
      component.confirmDelete(mockCategories[0]);
      expect(categoryServiceSpy.deleteCategory).not.toHaveBeenCalled();
    });

    it('should handle delete error', () => {
      categoryServiceSpy.deleteCategory.and.returnValue(throwError(() => new Error('fail')));
      component.confirmDelete(mockCategories[0]);
      expect(component.errorMessage()).toBeTruthy();
    });
  });
});
