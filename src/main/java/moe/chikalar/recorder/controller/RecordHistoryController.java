package moe.chikalar.recorder.controller;

import moe.chikalar.recorder.entity.RecordHistory;
import moe.chikalar.recorder.repo.RecordHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("recordHistory")
public class RecordHistoryController {
    @Autowired
    private RecordHistoryRepository recordHistoryRepository;


    @GetMapping("list")
    public String list(@RequestParam(defaultValue = "0") Integer page,
                       @RequestParam(defaultValue = "20") Integer pageSize,
                       Model model){

        Page<RecordHistory> p = recordHistoryRepository.
                findAll(PageRequest.of(page, pageSize, Sort.by(Sort.Order.desc("startTime"))));
        model.addAttribute("page", p);
        return "recordHistoryList";

    }

    @GetMapping("changeUploadStatus")
    public String updateUploadStatus(Long id, String uploadStatus){
       recordHistoryRepository.findById(id)
       .ifPresent(r->{
           r.setUploadStatus(uploadStatus);
           r.setUploadRetryCount(0);
           recordHistoryRepository.save(r);
       });
       return "redirect:/recordHistory/list";
    }
}
