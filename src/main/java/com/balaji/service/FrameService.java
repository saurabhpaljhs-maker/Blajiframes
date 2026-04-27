package com.balaji.service;

import com.balaji.dto.PriceCalculationDTO;
import com.balaji.exception.BalajiExceptions.*;
import com.balaji.model.*;
import com.balaji.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FrameService {

    private final FrameSizeRepository     frameSizeRepo;
    private final BeadingOptionRepository beadingRepo;
    private final CoverOptionRepository   coverRepo;

    public List<FrameSize>    getAllSizes()    { return frameSizeRepo.findAll(); }
    public List<BeadingOption> getAllBeadings() { return beadingRepo.findAll(); }
    public List<CoverOption>  getAllCovers()   { return coverRepo.findAll(); }

    public FrameSize getSizeById(Long id) {
        return frameSizeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("Frame size", id));
    }

    public BeadingOption getBeadingById(Long id) {
        return beadingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("Beading option", id));
    }

    public CoverOption getCoverById(Long id) {
        return coverRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("Cover option", id));
    }

    public PriceCalculationDTO calculatePrice(Long sizeId, Long beadingId, Long coverId) {
        FrameSize     size    = getSizeById(sizeId);
        BeadingOption beading = getBeadingById(beadingId);
        CoverOption   cover   = getCoverById(coverId);

        BigDecimal base  = size.getBasePrice();
        BigDecimal bAdd  = beading.getAdditionalPrice();
        BigDecimal cAdd  = cover.getAdditionalPrice();
        BigDecimal total = base.add(bAdd).add(cAdd);

        return PriceCalculationDTO.builder()
                .basePrice(base).beadingPrice(bAdd).coverPrice(cAdd)
                .totalPrice(total)
                .sizeLabel(size.getDisplayLabel())
                .beadingLabel(beading.getWidthLabel())
                .coverLabel(cover.getDisplayName())
                .build();
    }
}
