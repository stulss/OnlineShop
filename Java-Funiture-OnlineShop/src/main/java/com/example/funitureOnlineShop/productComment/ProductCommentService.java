package com.example.funitureOnlineShop.productComment;

import com.example.funitureOnlineShop.commentFile.CommentFile;
import com.example.funitureOnlineShop.commentFile.CommentFileRepository;
import com.example.funitureOnlineShop.core.error.exception.Exception404;
import com.example.funitureOnlineShop.core.error.exception.Exception500;
import com.example.funitureOnlineShop.option.Option;
import com.example.funitureOnlineShop.option.OptionRepository;
import com.example.funitureOnlineShop.user.User;
import com.example.funitureOnlineShop.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ProductCommentService {

    private final ProductCommentRepository productCommentRepository;
    private final UserRepository userRepository;
    private final OptionRepository optionRepository;
    private final CommentFileRepository commentFileRepository;
    // 파일 저장 경로
    private String filePath = "";

    // 상품 후기 저장
    @Transactional
    public ProductComment save(ProductCommentDto commentDto,
                               MultipartFile[] files) throws IOException {
        Optional<User> optionalUser = userRepository.findById(commentDto.getUserId());
        Optional<Option> optionalOption = optionRepository.findById(commentDto.getOptionId());
        // 없는 회원일 경우
        if (optionalUser.isEmpty())
            throw new Exception404("찾을 수 없는 회원 : " + commentDto.getUserId());
        // 등록되지 않은 옵션일 경우
        if (optionalOption.isEmpty())
            throw new Exception404("해당 옵션을 찾을 수 없습니다. : " + commentDto.getOptionId());
        User user = optionalUser.get();
        Option option = optionalOption.get();
        // 값 로딩 맞추기
        Hibernate.initialize(user);
        Hibernate.initialize(option);

        // 저장할 엔티티 생성
        ProductComment comment = commentDto.toEntity();
        try {
            ProductComment savedComment = productCommentRepository.save(comment);
            savedComment.updateFromEntity(user, option);

            // 추가
            if (!files[0].isEmpty()) {
                Path uploadPath = Paths.get(filePath);

                // 만약 경로가 없다면... 경로 생성
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                for (MultipartFile file : files) {
                    // 파일명 추출
                    String originalFilename = file.getOriginalFilename();

                    // 확장자 추출
                    String formatType = originalFilename.substring(
                            originalFilename.lastIndexOf("."));

                    // UUID 생성
                    String uuid = UUID.randomUUID().toString();

                    // 경로 지정
                    String path = filePath + uuid + originalFilename;

                    // 파일을 물리적으로 저장 (DB에 저장 X)
                    file.transferTo( new File(path) );

                    CommentFile commentFile = CommentFile.builder()
                            .filePath(filePath)
                            .fileName(originalFilename)
                            .uuid(uuid)
                            .fileType(formatType)
                            .fileSize(file.getSize())
                            .productComment(savedComment)
                            .build();

                    commentFileRepository.save(commentFile);
                }
            }

            return savedComment;
        } catch (Exception e) {
            throw new Exception500("상품 후기 저장 도중 오류 발생");
        }
    }

    // 상품의 상품 후기들을 탐색
    public List<ProductCommentDto> commentList(Long pId) {
        try {
            List<Option> optionList = optionRepository.findByProductId(pId);
            // 작성된 후기가 없는 경우
            if (optionList.isEmpty())
                return null;
            // 각 옵션의 후기들을 수집
            List<ProductCommentDto> commentDtos = new ArrayList<>();
            for (Option option : optionList) {
                List<ProductComment> comments = productCommentRepository.findAllByOptionId(option.getId());
                for (ProductComment comment : comments) {
                    // Dto로 바꾸어 넣기
                    commentDtos.add(ProductCommentDto.toSaveDto(comment));
                }
            }
            // 작성일 기준 최신순으로 정렬
            ProductCommentDto.sortByCreateDate(commentDtos);

            return commentDtos;
        } catch (Exception e) {
            throw new Exception500("상품 후기 탐색 중 오류 발생 : " + pId);
        }
    }
}
