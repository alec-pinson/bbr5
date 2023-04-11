package uk.co.bbr.web.years;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.co.bbr.services.years.YearService;
import uk.co.bbr.services.years.sql.dto.YearListEntrySqlDto;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class YearController {

    private final YearService yearService;

    @GetMapping("/years")
    public String contestYearHome(Model model){
        List<YearListEntrySqlDto> allYears = this.yearService.fetchFullYearList();

        model.addAttribute("Years", allYears);

        return "years/home";
    }

}
