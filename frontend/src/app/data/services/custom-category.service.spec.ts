import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppSettings } from '../../shared/constants';
import { CustomCategoryService } from './custom-category.service';

describe('CustomCategoryService', () => {
  let service: CustomCategoryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), CustomCategoryService]
    });

    service = TestBed.inject(CustomCategoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get my categories', () => {
    service.getMyCategories().subscribe(response => {
      expect(response.length).toBe(1);
      expect(response[0].category).toBe('Sub-12');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/custom-age-categories`);
    expect(request.request.method).toBe('GET');

    request.flush([{ id: 1, category: 'Sub-12', description: 'Under 12', custom: true }]);
  });

  it('should create a category', () => {
    service.createCategory('Sub-14').subscribe(response => {
      expect(response.category).toBe('Sub-14');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/custom-age-categories`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Sub-14' });

    request.flush({ id: 2, category: 'Sub-14', description: 'Under 14', custom: true });
  });

  it('should update a category', () => {
    service.updateCategory(1, 'Sub-14 Updated').subscribe(response => {
      expect(response.category).toBe('Sub-14 Updated');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/custom-age-categories/1`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Sub-14 Updated' });

    request.flush({ id: 1, category: 'Sub-14 Updated', description: 'Under 14', custom: true });
  });

  it('should delete a category', () => {
    service.deleteCategory(1).subscribe(response => {
      expect(response).toBeNull();
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/custom-age-categories/1`);
    expect(request.request.method).toBe('DELETE');

    request.flush(null);
  });
});
