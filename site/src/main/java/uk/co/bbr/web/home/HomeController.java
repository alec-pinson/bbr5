package uk.co.bbr.web.home;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.co.bbr.services.security.UserService;
import uk.co.bbr.services.security.dao.SiteUserDao;
import uk.co.bbr.services.statistics.StatisticsService;
import uk.co.bbr.services.statistics.dto.StatisticsDto;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final StatisticsService statisticsService;

    @GetMapping("/")
    public String home(Model model) {
        return "home/home";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        StatisticsDto statistics = this.statisticsService.fetchStatistics();

        model.addAttribute("Statistics", statistics);
        return "home/statistics";
    }

    @GetMapping("/faq")
    public String faq() {
        return "home/faq";
    }

    @GetMapping("/about-us")
    public String aboutUs() {
        return "home/about-us";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "home/privacy";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {

        List<SiteUserDao> topUsers = this.userService.fetchTopUsers();

        model.addAttribute("TopUsers", topUsers);

        return "home/leaderboard";
    }
}
