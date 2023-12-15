package com.example.funitureOnlineShop.option;

import com.example.funitureOnlineShop.core.error.exception.Exception500;
import com.example.funitureOnlineShop.order.Order;
import com.example.funitureOnlineShop.order.item.Item;
import com.example.funitureOnlineShop.product.Product;
import com.example.funitureOnlineShop.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    // ** 상품ID를 기반으로 옵션을 저장, 없을 시 예외처리
    @Transactional
    public Option save(OptionResponse.FindByProductIdDTO requestDTO) {
        Optional<Product> option = productRepository.findById(requestDTO.getProductId());
        Product product = option.orElseThrow(() ->
                new Exception500("상품을 찾을 수 없습니다. 상품 ID: " + requestDTO.getProductId()));

        Option optionEntity = requestDTO.toEntity();
        optionEntity.toUpdate(product);

        return optionRepository.save(optionEntity);
    }

    // ** 개별 옵션 검색, 없을 시 예외처리
    public List<OptionResponse.FindByProductIdDTO> findByProductId(Long id) {
        List<Option> optionList = optionRepository.findByProductId(id);
        if (optionList.isEmpty()) {
            throw new Exception500("상품에 해당하는 옵션이 없습니다. 상품 ID: " + id);
        }
        List<OptionResponse.FindByProductIdDTO> dtos =
                optionList.stream().map(OptionResponse.FindByProductIdDTO::new)
                        .collect(Collectors.toList());
        return dtos;
    }
    // ** 전체 옵션 검색, 없을 시 예외처리
    public List<OptionResponse.FindAllDTO> findAll(){
        List<Option> optionList = optionRepository.findAll();
        if (optionList.isEmpty()) {
            throw new Exception500("옵션이 없습니다.");
        }
        List<OptionResponse.FindAllDTO> dtos =
                optionList.stream().map(OptionResponse.FindAllDTO::new)
                        .collect(Collectors.toList());
        return dtos;
    }
    // ** 옵션 변경
    @Transactional
    public void update(OptionResponse.FindAllDTO requestDTO){
        try {
            Optional<Option> optionalOption = optionRepository.findById(requestDTO.getId());

            if (optionalOption.isPresent()) {
                Option option = optionalOption.get();
                option.updateFromDTO(requestDTO);

                optionRepository.save(option);
            } else {
                throw new Exception500("옵션을 찾을 수 없습니다. 옵션 ID: " + requestDTO.getId());
            }
        } catch (Exception e) {
            throw new Exception500("옵션 업데이트 중에 오류가 발생했습니다.");
        }
    }
    // ** 옵션 삭제
    @Transactional
    public void delete(Long id){
        optionRepository.deleteById(id);
    }

    // ** 옵션 수량 업데이트
    @Transactional
    public void updateStock(Long optionId, Long newStockQuantity) {
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new Exception500("옵션이 존재하지 않습니다. 옵션 ID: " + optionId));
        option.updateStockQuantity(newStockQuantity);

        optionRepository.save(option);
    }
    // 옵션 수량 차감
    @Transactional
    public void deductStock(Long optionId, Long quantity) {
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new Exception500("옵션이 존재하지 않습니다. 옵션 ID: " + optionId));

        Long currentStock = option.getStockQuantity();
        if (currentStock >= quantity) {
            option.updateStockQuantity(currentStock - quantity);
            optionRepository.save(option);
        } else {
            throw new Exception500("재고가 부족합니다. 옵션 ID: " + optionId);
        }
    }
    // 결제 완료 후 재고 차감
    @Transactional
    public void deductStockOnOrder(Order order) {
        for (Item orderItem : order.getOrderItems()) {
            Long optionId = orderItem.getOption().getId();
            Long quantity = orderItem.getQuantity();
            deductStock(optionId, quantity);
        }
    }
    // 옵션 수량 복구
    @Transactional
    public void restoreStock(Long optionId, Long quantity) {
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new Exception500("옵션이 존재하지 않습니다. 옵션 ID: " + optionId));

        option.updateStockQuantity(option.getStockQuantity() + quantity);
        optionRepository.save(option);
    }

    // 주문 취소 시 재고 복구
    @Transactional
    public void restoreStockOnOrderCancel(Order order) {
        for (Item orderItem : order.getOrderItems()) {
            Long optionId = orderItem.getOption().getId();
            Long quantity = orderItem.getQuantity();
            restoreStock(optionId, quantity);
        }
    }
}
