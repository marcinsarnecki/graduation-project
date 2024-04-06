package uwr.ms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uwr.ms.model.entity.FriendshipEntity;
import uwr.ms.service.AppUserService;
import uwr.ms.service.FriendshipService;

import java.util.List;
import java.util.Scanner;

@SpringBootApplication(scanBasePackages = {"uwr.ms"})
public class TravelPlannerApplication {
    @Bean
    @Profile("dev")
    public CommandLineRunner commandLineRunner(AppUserService appUserService, FriendshipService friendshipService) { // ONLY FOR DEV PURPOSES, NOT IN DOCKER/PRODUCTION !!!
        return args -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.println("aaa");
                    String input = scanner.next();

                    if ("1".equals(input)) {
                        String user1 = scanner.next();
                        String user2 = scanner.next();
                        friendshipService.sendFriendRequest(user1, user2);

                    } else if("2".equals(input)) {
                        String user1 = scanner.next();
                        List<FriendshipEntity> friendshipEntityList = friendshipService.listReceivedFriendRequests(user1);
                        for(FriendshipEntity friendshipEntity: friendshipEntityList) {
                            System.out.println(friendshipEntity.toString());
                        }
                    }
                    else if("3".equals(input)) {
                        String user1 = scanner.next();
                        appUserService.deleteUser(user1);
                    }else if ("q".equals(input)) {
                        break;
                    }
                }
            }
        };
    }
    public static void main(String[] args) {
        SpringApplication.run(TravelPlannerApplication.class, args);
    }
}

