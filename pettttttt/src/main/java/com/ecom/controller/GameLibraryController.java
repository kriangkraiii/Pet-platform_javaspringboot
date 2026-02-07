package com.ecom.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecom.model.Category;
import com.ecom.model.GameLibrary;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.GameLibraryService;
import com.ecom.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class GameLibraryController {

	@Autowired
	private GameLibraryService gameLibraryService;

	@Autowired
	private UserService userService;

	@Autowired
	private CartService cartService;

	@Autowired
	private CategoryService categoryService;

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			try {
				String email = p.getName();
				UserDtls userDtls = userService.getUserByEmail(email);
				if (userDtls != null) {
					m.addAttribute("user", userDtls);
					Integer countCart = cartService.getCountCart(userDtls.getId());
					m.addAttribute("countCart", countCart != null ? countCart : 0);
				}
			} catch (Exception e) {
				e.printStackTrace();
				m.addAttribute("countCart", 0);
			}
		}

		try {
			List<Category> allActiveCategory = categoryService.getAllActiveCategory();
			m.addAttribute("categorys", allActiveCategory != null ? allActiveCategory : new ArrayList<>());
		} catch (Exception e) {
			e.printStackTrace();
			m.addAttribute("categorys", new ArrayList<>());
		}
	}

	@GetMapping("/user/game-library")
	public String gameLibrary(Principal p, Model m) {
		if (p == null) {
			return "redirect:/signin";
		}

		String email = p.getName();
		UserDtls user = userService.getUserByEmail(email);

		List<GameLibrary> games = gameLibraryService.getGamesByUser(user.getId());
		m.addAttribute("games", games);
		m.addAttribute("gamesCount", games.size());

		return "user/game_library";
	}

	@GetMapping("/user/game-library/download/{id}")
	public String downloadGame(@PathVariable Integer id, Principal p, HttpSession session) {
		if (p == null) {
			return "redirect:/signin";
		}

		String email = p.getName();
		UserDtls user = userService.getUserByEmail(email);

		// Mark as downloaded
		gameLibraryService.markAsDownloaded(id);

		// In a real app, you'd redirect to the actual download link
		// For now we redirect back to library with a success message
		session.setAttribute("succMsg", "Game download started!");

		return "redirect:/user/game-library";
	}
}
