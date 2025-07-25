package gift.controller;

import gift.dto.OptionRequest;
import gift.service.OptionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/products/{productId}/options")
public class OptionAdminController {

    private final OptionService optionService;

    public OptionAdminController(OptionService optionService) {
        this.optionService = optionService;
    }

    /**
     * 옵션 등록 폼
     */
    @GetMapping("/new")
    public String newForm(@PathVariable Long productId, Model model) {
        model.addAttribute("productId", productId);
        model.addAttribute("optionRequest", new OptionRequest("", 1));
        return "admin/option/create-option-form";
    }

    /**
     * 옵션 등록 처리
     */
    @PostMapping("/new")
    public String create(@PathVariable Long productId,
                         @ModelAttribute("optionRequest") @Valid OptionRequest request,
                         BindingResult bindingResult,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", productId);
            return "admin/option/create-option-form";
        }

        try {
            optionService.createOption(productId, request.name(), request.quantity());
        } catch (IllegalArgumentException e) {
            model.addAttribute("productId", productId);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/option/create-option-form";
        }

        return "redirect:/admin/products";
    }

    /**
     * 옵션 삭제 처리
     */
    @PostMapping("/{optionId}/delete")
    public String delete(@PathVariable Long productId,
                         @PathVariable Long optionId) {
        optionService.deleteOption(optionId);
        return "redirect:/admin/products/" + productId + "/edit";
    }
}
