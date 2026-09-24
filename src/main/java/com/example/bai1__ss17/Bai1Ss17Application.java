package com.example.bai1__ss17;

import com.example.bai1__ss17.model.Dish;
import com.example.bai1__ss17.service.FoodService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.util.List;

@SpringBootApplication
@EnableCaching
public class Bai1Ss17Application {

    public static void main(String[] args) {
        SpringApplication.run(Bai1Ss17Application.class, args);
    }

    @Bean
    @Profile("!test")
    public CommandLineRunner runDemo(FoodService foodService) {
        return args -> {
            System.out.println("\n==================================================================");
            System.out.println("========== BẮT ĐẦU KIỂM TRA DEMO DISTRIBUTED REDIS CACHE =========");
            System.out.println("==================================================================");

            // Lần gọi 1 cho Nhà hàng 1 (Chưa có trong cache -> Sẽ gọi vào method DB)
            System.out.println("\n[LẦN GỌI 1] Lấy danh sách món ăn cho Nhà hàng ID = 1 (Lần đầu - Cache Miss):");
            List<Dish> list1 = foodService.getDishesByRestaurant(1L);
            System.out.println("Kết quả nhận được (" + list1.size() + " món): " + list1);

            // Lần gọi 2 cho Nhà hàng 1 (Đã có trong cache -> Sẽ lấy từ Redis, KHÔNG gọi method)
            System.out.println("\n[LẦN GỌI 2] Lấy lại danh sách món ăn cho Nhà hàng ID = 1 (Lần 2 - Cache Hit):");
            List<Dish> list2 = foodService.getDishesByRestaurant(1L);
            System.out.println("Kết quả nhận được (" + list2.size() + " món - từ Redis Cache): " + list2);

            // Lần gọi 3 cho Nhà hàng 2 (Chưa có trong cache -> Cache Miss)
            System.out.println("\n[LẦN GỌI 3] Lấy danh sách món ăn cho Nhà hàng ID = 2 (Lần đầu - Cache Miss):");
            List<Dish> list3 = foodService.getDishesByRestaurant(2L);
            System.out.println("Kết quả nhận được (" + list3.size() + " món): " + list3);

            System.out.println("\n==================================================================");
            System.out.println("====== DEMO HOÀN TẤT - KIỂM TRA TRONG REDIS-CLI: KEYS / GET ======");
            System.out.println("==================================================================\n");
        };
    }
}
