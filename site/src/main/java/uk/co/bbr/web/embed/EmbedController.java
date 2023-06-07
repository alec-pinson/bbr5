package uk.co.bbr.web.embed;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.co.bbr.services.bands.BandService;
import uk.co.bbr.services.bands.dao.BandDao;
import uk.co.bbr.services.bands.dto.BandDetailsDto;
import uk.co.bbr.services.bands.types.ResultSetCategory;
import uk.co.bbr.services.events.ContestResultService;
import uk.co.bbr.services.events.dao.ContestResultDao;
import uk.co.bbr.services.framework.NotFoundException;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class EmbedController {

    private final BandService bandService;
    private final ContestResultService contestResultService;

    @GetMapping("/embed/band/{bandSlug:[\\-a-z\\d]{2,}}/results-{type:all|non-whit|whit}/2023")
    public String embedBandResults(Model model, @PathVariable("bandSlug") String bandSlug, @PathVariable("type") String type) {
        Optional<BandDao> band = this.bandService.fetchBySlug(bandSlug);
        if (band.isEmpty()) {
            throw NotFoundException.bandNotFoundBySlug(bandSlug);
        }

        BandDetailsDto bandResults = this.contestResultService.findResultsForBand(band.get(), ResultSetCategory.PAST);

        List<ContestResultDao> resultsToReturn;
        switch (type) {
            case "non-whit" -> resultsToReturn = bandResults.getBandNonWhitResults();
            case "whit" -> resultsToReturn = bandResults.getBandWhitResults();
            default -> resultsToReturn = bandResults.getBandAllResults();
        }

        model.addAttribute("Band", band.get());
        model.addAttribute("BandSlugUnderscores", band.get().getSlug().replace("-", "_"));
        model.addAttribute("Results", resultsToReturn);

        return "embed/band-2023";
    }
}

