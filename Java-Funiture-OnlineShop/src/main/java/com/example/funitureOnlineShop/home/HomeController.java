package com.example.funitureOnlineShop.home;

import com.example.funitureOnlineShop.Board.BoardDTO;
import com.example.funitureOnlineShop.Board.BoardService;
import com.example.funitureOnlineShop.category.CategoryResponse;
import com.example.funitureOnlineShop.category.CategoryService;
import com.example.funitureOnlineShop.fileProduct.FileProductResponse;
import com.example.funitureOnlineShop.orderCheck.OrderCheckDto;
import com.example.funitureOnlineShop.product.ProductResponse;
import com.example.funitureOnlineShop.product.ProductService;
import com.example.funitureOnlineShop.productComment.ProductCommentResponse;
import com.example.funitureOnlineShop.productComment.ProductCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Controller
public class HomeController {

    private final ProductService productService;
    private final ProductCommentService productCommentService;
    private final BoardService boardService;
    private final CategoryService categoryService;

    // 메인 홈페이지
    @GetMapping(value = {"/", ""})
    public String home() {
        return "index";
    }
    // 카테고리 생성
    @GetMapping("/categorycreate")
    public String categoryCreate(Model model) {
        List<CategoryResponse.FindAllDto> categories = categoryService.findAllSuper();
        model.addAttribute("categories", categories);
        return "categorycreate";
    }

    @GetMapping("/category/updateForm")
    public String categoryUdate(Model model) {
        List<CategoryResponse.FindAllDto> dtos = categoryService.findAllSuper();
        model.addAttribute("categories", dtos);

        return "categoryUpdate";
    }

    // !!----------< 상품 관련 페이지 > -----------

    // 상품 상세 페이지
    @GetMapping("/product/show/{id}")
    public String showProduct(@PathVariable Long id, Model model) {
        ProductResponse.FindByIdDTO findByIdDTO = productService.findById(id);
        model.addAttribute("product", findByIdDTO);
        return "productPage";
    }

    // 카테고리 클릭시 특정 카테고리 상품 확인
    @GetMapping("/category/show/{id}")
    public String showProductByCategory(@PathVariable Long id) {
        return "productCategoryPage";
    }

    // !< 관리자용 > 상품 신규 생성 페이지
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/product/add")
    public String showProductCreate(Model model) {
        List<CategoryResponse.FindAllDto> categories = categoryService.findAllSuper();
        model.addAttribute("categories", categories);
        return "productCreate";
    }

    // !< 관리자용 > 상품 신규 수정 페이지
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/product/update")
    public String showProductUpdate() {
        return "productUpdate";
    }

    // !!----------< 장바구니 관련 페이지 > -----------

    // 로그인한 사용자의 장바구니 확인
    @GetMapping("/cart")
    public String showCart() {
        return "cartPage";
    }

    // !!----------< 결제 관련 페이지 > -----------
    // 결제 상세 페이지
    @GetMapping("/order")
    public String showOrder() {
        return "orderPage";
    }

    // !!----------< 유저 관련 페이지 > -----------
    // 각 유저의 마이페이지
    @GetMapping("/myPage")
    public String showUserInfo() {
        return "myPage";
    }

    @GetMapping("/product_comment/update/{id}")
    public String updateCommentForm(@PathVariable Long id, Model model){
        ProductCommentResponse.CommentDto dto = productCommentService.findById(id);
        model.addAttribute("comment", dto);
        return "commentUpdate";
    }

    @GetMapping("/product_comment/save/{id}")
    public String writeComment(@PathVariable Long id, Model model){
        OrderCheckDto orderCheckDto = productCommentService.findOrderCheck(id);
        model.addAttribute("orderCheck", orderCheckDto);
        return "productReview";
    }

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @GetMapping("/payments/cancel")
    public String payCancel() {
        return "paycancel";
    }

    @GetMapping("/payments/index")
    public String payIndex() {
        return "payindex";
    }

    @GetMapping("/payments/response")
    public String payResponse() {
        return "payresponse";
    }

    @GetMapping("/join")
    public String joinForm() {
        return "join";
    }

    @GetMapping("/adminPage")
    public String adminPage(){
        return "/adminPage";
    }

    @GetMapping("/menu")
    public String menu(Model model) {
        List<CategoryResponse.FindAllDto> categories = categoryService.findAllSuper();
        model.addAttribute("categories", categories);

        List<CategoryResponse.FindAllDto> parents = new ArrayList<>();
        for (CategoryResponse.FindAllDto dto : categories) {
            parents.addAll(categoryService.findAllSon(dto.getId()));
        }
        model.addAttribute("parents", parents);

        List<CategoryResponse.FindAllDto> sons = new ArrayList<>();
        for (CategoryResponse.FindAllDto dto : parents) {
            sons.addAll(categoryService.findAllSon(dto.getId()));
        }
        model.addAttribute("sons", sons);

        return "menu";
    }
}
