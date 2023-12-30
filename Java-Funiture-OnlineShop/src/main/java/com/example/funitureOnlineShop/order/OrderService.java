package com.example.funitureOnlineShop.order;

import com.example.funitureOnlineShop.cart.Cart;
import com.example.funitureOnlineShop.cart.CartRepository;
import com.example.funitureOnlineShop.core.error.exception.Exception404;
import com.example.funitureOnlineShop.core.error.exception.Exception500;
import com.example.funitureOnlineShop.option.OptionService;
import com.example.funitureOnlineShop.order.item.Item;
import com.example.funitureOnlineShop.order.item.ItemRepository;
import com.example.funitureOnlineShop.orderCheck.OrderCheck;
import com.example.funitureOnlineShop.orderCheck.OrderCheckDto;
import com.example.funitureOnlineShop.orderCheck.OrderCheckRepository;
import com.example.funitureOnlineShop.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;
    private final OptionService optionService;
    private final OrderCheckRepository orderCheckRepository;
    // 결제 시도시 작동
    @Transactional
    public OrderResponse.FindByIdDTO save(User user) {
        //장바구니 조회
        List<Cart> cartList = cartRepository.findAllByUserId(user.getId());

        if(cartList.isEmpty()){
            throw new Exception404("장바구니에 상품 내역이 존재하지 않습니다.");
        }

        // 주문 생성
        Order order = Order.builder()
                .user(user)
                .orderDate(LocalDateTime.now())
                .build();
        List<Item> itemList = new ArrayList<>();

        try {
            order = orderRepository.save(order);

            // 아이템 저장
            for (Cart cart : cartList) {
                Item item = Item.builder()
                        .option(cart.getOption())
                        .order(order)
                        .quantity(cart.getQuantity())
                        .price(cart.getPrice())
                        .build();

                itemList.add(item);
            }

            itemRepository.saveAll(itemList);
            cartRepository.deleteAll(cartList);
        } catch (Exception e){
            throw new Exception500("주문 생성중 오류가 발생하였습니다.");
        }
        return new OrderResponse.FindByIdDTO(order, itemList);
    }

    public OrderResponse.FindByIdDTO findById(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(
            () -> new Exception404("해당주문 내역을 찾을 수 없습니다."+ id));

        List<Item> itemList = itemRepository.findAllByOrderId(id);
        return new OrderResponse.FindByIdDTO(order,itemList);
    }

    @Transactional
    public void delete(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new Exception404("주문을 찾을 수 없습니다."));

        List<Item> itemsToDelete = itemRepository.findAllByOrderId(orderId);

        try {
            optionService.deductStock(itemsToDelete);
            orderRepository.delete(order);
            itemRepository.deleteAll(itemsToDelete);
        } catch (Exception e) {
            throw new Exception500("주문 및 주문 항목 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ** 페이먼트 관련 기능 추가 ( 작업 : 이아현)

    public OrderCheckDto findOrderChecks(Long checkId) {
        Optional<OrderCheck> optionalOrderCheck = orderCheckRepository.findById(checkId);
        if (optionalOrderCheck.isEmpty())
            throw new Exception500("아이디 못받아옴ㅋ");
        OrderCheck orderCheck = optionalOrderCheck.get();

        return OrderCheckDto.toOrderCheckDto(orderCheck, null);
    }
}
