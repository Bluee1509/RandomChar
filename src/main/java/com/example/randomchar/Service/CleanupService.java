package com.example.randomchar.Service;

import com.example.randomchar.Repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CleanupService {

    private final UserRepository userRepository;

    public CleanupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    // chạy lúc 2 giờ sáng mỗi ngày
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupUnverifiedAccounts() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        userRepository.deleteUnverifiedUsersOlderThan(cutoffTime);

        System.out.println("🧹 Đã dọn dẹp các tài khoản chưa kích hoạt quá 24h!");
    }
}