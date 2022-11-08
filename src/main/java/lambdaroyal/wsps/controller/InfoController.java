package lambdaroyal.wsps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lambdaroyal.wsps.Context;
import lambdaroyal.wsps.PrintServiceRepository;
import lambdaroyal.wsps.VersionHolder;

@Controller
public class InfoController {
	@Autowired
	PrintServiceRepository printServiceRepository;
	
	@Autowired
	Context context;
	
	@Autowired
	VersionHolder versionHolder;
	
	@GetMapping("/info")
	public String info(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
		model.addAttribute("printers", printServiceRepository.getAllPrinters());
		model.addAttribute("context", context);
		model.addAttribute("version", versionHolder.getVersion());
		return "info";
	}

}