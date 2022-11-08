package lambdaroyal.wsps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lambdaroyal.wsps.Context;
import lambdaroyal.wsps.PrintServiceRepository;

@Controller
public class InfoController {
	@Autowired
	PrintServiceRepository printServiceRepository;
	
	@Autowired
	Context context;
	
	@GetMapping("/info")
	public String info(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
		model.addAttribute("printers", printServiceRepository.getAllPrinters());
		model.addAttribute("context", context);
		model.addAttribute("version", null);
		return "info";
	}

}