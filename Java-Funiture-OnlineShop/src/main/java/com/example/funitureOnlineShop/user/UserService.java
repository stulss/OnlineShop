package com.example.funitureOnlineShop.user;

import com.example.funitureOnlineShop.core.error.exception.Exception400;
import com.example.funitureOnlineShop.core.error.exception.Exception401;
import com.example.funitureOnlineShop.core.error.exception.Exception500;
import com.example.funitureOnlineShop.core.security.CustomUserDetails;
import com.example.funitureOnlineShop.core.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Transactional(readOnly = true)
// 메서드나 클래스에 적용가능.
// Transactional
// 어노테이션이 적용된 메서드가 호출되면, 새로운 트랜잭션이 시작됨.
// 메서드 실행이 성공적으로 완료되면, 트랜잭션은 자동으로 커밋.
// 메서드 실행 중에 예외가 발생하면, 트랜잭션은 자동으로 롤백.
//
// readOnly = true : 이 설정은 해당 트랜잭션이 데이터를 변경하지 않고 읽기전용으로만 사용이 가능하다는것을 명시적으로 나타냄.
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void join(UserRequest.JoinDto joinDto) {
        // 이미 있는 이메일인지 확인
        checkEmail(joinDto.getEmail());

        // 입력한 비밀번호를 인코딩하여 넣음
        String encodedPassword = passwordEncoder.encode(joinDto.getPassword());
        joinDto.setPassword(encodedPassword);

        try {
            // 회원 가입
            userRepository.save(joinDto.toEntity());


            // 자기 전화번호로 회원가입 메세지가 오도록 함 (돈 내야 해서 지금은 안 씀)
            // SignUpMessageSender.sendMessage("01074517172", joinDto.getPhoneNumber(),"환영합니다. 회원가입이 완료되었습니다.");
        } catch (Exception e) {
            throw new Exception500(e.getMessage());
        }
    }
    @Transactional
    public void joinAdmin(UserRequest.JoinAdminDto DTO){
        checkEmail(DTO.getEmail());

        String encodedPassword = passwordEncoder.encode(DTO.getPassword());
        DTO.setPassword(encodedPassword);

        try{
            User user = DTO.toEntity();
            user.addAdminRole();
            userRepository.save(user);
        }catch (Exception e) {
            throw new Exception500(e.getMessage());
        }
    }

    // 이미 존재하는 이메일인지 확인
    public void checkEmail(String email){
        Optional<User> users = userRepository.findByEmail(email);
        if (users.isPresent()){
            throw new Exception400("이미 존재하는 이메일입니다. : " + email);
        }
    }


    // id, 비밀번호 인증 후 access_token 생성
    @Transactional
    public String login(UserRequest.JoinDto joinDto, HttpServletResponse res) {
        // 인증 작업
        try{
            UsernamePasswordAuthenticationToken token
                    = new UsernamePasswordAuthenticationToken(
                    joinDto.getEmail(), joinDto.getPassword());

            // anonymousUser = 비인증
            Authentication authentication
                    = authenticationManager.authenticate(token);
            // 인증 완료 값을 받아온다.
            // 인증키
            CustomUserDetails customUserDetails = (CustomUserDetails)authentication.getPrincipal();

            // 토큰 생성 및 저장
            User user = customUserDetails.getUser();
            String prefixJwt = JwtTokenProvider.create(user);
            String accessToken = prefixJwt.replace(JwtTokenProvider.TOKEN_PREFIX,"");
            String refreshToken = JwtTokenProvider.createRefresh(user);
            user.setRefreshToken(refreshToken);
            setCookie(res, "token", accessToken);

            return prefixJwt;
        }catch (Exception e){
            throw new Exception401("인증되지 않음.");
        }
    }

    // 쿠키 설정
    public void setCookie(HttpServletResponse res, String name, String value){
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        res.addCookie(cookie);
    }

    // 쿠키 삭제
    public void deleteCookie(HttpServletResponse res, String name){
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        res.addCookie(cookie);
    }

    // 로그아웃
    @Transactional
    public String logout(Long id, HttpServletResponse res) {
        try {
            User user = userRepository.findById(id).orElseThrow();
            killToken(user);
            deleteCookie(res, "token");

            // 메인 화면으로
            return "/";
        } catch (Exception e){
            throw new Exception500(e.getMessage());
        }
    }

    // 토큰 삭제
    public void killToken(User user){
        // DB에서 갱신 토큰 삭제
        user.setRefreshToken(null);
        // 사용한 토큰의 재사용 막기 (블랙리스트로 이동)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtTokenProvider.invalidateToken(authentication);
    }

    public UserResponse.UserDTO getUserInfo(UserRequest.UserInfoDto userInfoDto) {
        String email = userInfoDto.getEmail();
        String username = userInfoDto.getUsername();

        // 이메일과 이름을 기반으로 사용자 정보를 조회
        User user = userRepository.findByEmailAndUsername(email, username)
                .orElseThrow(() -> new Exception500("사용자를 찾을 수 없습니다."));

        return UserResponse.UserDTO.fromEntity(user);
    }
}
