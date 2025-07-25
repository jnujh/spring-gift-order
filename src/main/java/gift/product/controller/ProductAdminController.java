package gift.product.controller;

import gift.product.domain.Product;
import gift.option.dto.OptionRequest;
import gift.option.dto.OptionResponse;
import gift.product.dto.ProductRequest;
import gift.product.dto.ProductResponse;
import gift.option.service.OptionService;
import gift.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Controller
@RequestMapping("/admin/products")
public class ProductAdminController {

    private final ProductService productService;
    private final OptionService optionService;

    public ProductAdminController(ProductService productService, OptionService optionService) {
        this.productService = productService;
        this.optionService = optionService;
    }

    /**
     * 상품 목록 (검색)
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 3, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            Model model
    ) {
        Page<Product> page = productService.search(keyword, pageable);
        List<ProductResponse> products = page.map(ProductResponse::from).getContent();

        model.addAttribute("products", products);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        return "admin/product/list";
    }


    /**
     * 상품 등록 폼
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("productRequest", new ProductRequest("", 0, "", new ArrayList<>()));
        return "admin/product/create-product-form";
    }

    /**
     * 상품 등록 처리
     */
    @PostMapping
    public String create(@ModelAttribute @Valid ProductRequest request,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productRequest", request);
            model.addAttribute("errors", bindingResult);
            return "admin/product/create-product-form";
        }

        try {
            productService.create(request);
        } catch (IllegalArgumentException e) {
            model.addAttribute("productRequest", request);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/product/create-product-form";
        }

        return "redirect:/admin/products";
    }

    /**
     * 상품 수정 폼
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        ProductRequest request = new ProductRequest(
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getOptions().stream()
                        .map(option -> new OptionRequest(option.getName(), option.getQuantity()))
                        .collect(toList())
        );
        model.addAttribute("productId", product.getId());
        model.addAttribute("productRequest", request);

        List<OptionResponse> options = optionService.getOptionsByProductId(id);
        model.addAttribute("options", options);

        return "admin/product/edit-product-form";
    }

    /**
     * 상품 수정 처리
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute @Valid ProductRequest request,
                         BindingResult bindingResult,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id);
            model.addAttribute("productRequest", request);
            model.addAttribute("errors", bindingResult);
            return "admin/product/edit-product-form";
        }

        try {
            productService.update(id, request.name(), request.price(), request.imageUrl());
        } catch (IllegalArgumentException e) {
            model.addAttribute("productRequest", request);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/product/edit-product-form";
        }

        return "redirect:/admin/products";
    }

    /**
     * 상품 삭제 처리
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/products";
    }
}
