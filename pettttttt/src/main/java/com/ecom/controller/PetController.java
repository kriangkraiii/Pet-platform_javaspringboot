package com.ecom.controller;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Pet;
import com.ecom.model.UserDtls;
import com.ecom.service.FileService;
import com.ecom.service.PetService;
import com.ecom.service.UserService;
import com.ecom.util.BucketType;
import com.ecom.util.CommonUtil;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/pet")
public class PetController {

	@Autowired
	private PetService petService;

	@Autowired
	private UserService userService;
	
	@Autowired
	private FileService fileService;
	
	@Autowired
	private CommonUtil commonUtil;
	
	@Autowired
	private CategoryService categoryService;
	
	@Autowired
	private CartService cartService;

	@ModelAttribute("user")
	public UserDtls getLoggedInUser(Principal principal) {
		if (principal != null) {
			return userService.getUserByEmail(principal.getName());
		}
		return null;
	}
	
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

	@GetMapping
	public String showPets(Model model, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}

		String email = principal.getName();
		UserDtls user = userService.getUserByEmail(email);

		List<Pet> pets = petService.getUserPets(user);
		model.addAttribute("pets", pets);

		if (pets == null || pets.isEmpty()) {
			model.addAttribute("noPetsMsg", "You have no pets added yet.");
		}

		return "pet";
	}

	@GetMapping("/add")
	public String showAddPetForm(Model model, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}

		String email = principal.getName();
		UserDtls user = userService.getUserByEmail(email);

		if (user == null) {
			return "redirect:/login";
		}

		model.addAttribute("pet", new Pet());
		return "add_pet"; 
	}
	// Add new pet
	@PostMapping("/add")
	public String addPet(@RequestParam String name, @RequestParam String type, @RequestParam String breed,  Principal principal,
			@RequestParam(required = false) String description, @RequestParam("imagePet") MultipartFile imageFile,
			HttpSession session) {

		if (principal == null) {
			return "redirect:/login";
		}

		String email = principal.getName();
		UserDtls user = userService.getUserByEmail(email);

		if (user == null) {
			return "redirect:/login";
		}

		//String imagePath = "/img/pet_img/default.jpg"; // default image path

		String imageUrl = commonUtil.getImageUrl(imageFile, BucketType.PETPROFILE.getId());

		if (!imageFile.isEmpty()) {
//				String originalFilename = imageFile.getOriginalFilename();
//				String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
//
//				if (!List.of("jpg", "jpeg", "png", "gif", "webp").contains(fileExtension)) {
//					session.setAttribute("errorImagePMsg", "only image files are allowed (jpg, jpeg, png, gif, webp)");
//					return "redirect:/user/pet";
//				}
//
//				String fileName = UUID.randomUUID().toString() + "_" + originalFilename.replaceAll("\\s+", "_");
//				String uploadDir = System.getProperty("user.dir") + "/uploads/pet_img/";
//				
//				File uploadFolder = new File(uploadDir);
//				if (!uploadFolder.exists()) {
//					uploadFolder.mkdirs();
//				}
//
//				Path filePath = Paths.get(uploadDir, fileName);
//				Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//				//String imagePet = fileName;  
			petService.addPet(name, type, breed, user, description, imageUrl);
			
		}
		session.setAttribute("succAPMsg", "Pet added successfully!");
		fileService.uploadFileS3(imageFile	, 4);

		return "redirect:/user/pet";
	}

	// Delete pet
	@PostMapping("/delete/{id}")
	public String deletePet(@PathVariable("id") int petId, HttpSession session, Principal principal) {
	    try {
	        if (principal == null) {
	            return "redirect:/signin";
	        }

	        String email = principal.getName();
	        UserDtls user = userService.getUserByEmail(email);
	        
	        if (user == null) {
	            return "redirect:/signin";
	        }

	        Pet pet = petService.getPetById(petId);
	        if (pet == null || !pet.getOwner().getId().equals(user.getId())) {
	            session.setAttribute("errorNotPMsg", "Pet not found or you don't have permission to delete this pet");
	            return "redirect:/user/pet";
	        }

	        // Delete image file if not default
	        if (pet.getImagePet() != null && !pet.getImagePet().equals("/img/pet_img/default.jpg")) {
	            try {
	                String imagePath = "src/main/resources/static" + pet.getImagePet();
	                Files.deleteIfExists(Paths.get(imagePath));
	            } catch (Exception e) {
	                // Log but don't fail the deletion
	                System.err.println("Failed to delete image file: " + e.getMessage());
	            }
	        }

	        petService.deletePet(petId);
	        session.setAttribute("succDPMsg", "Pet deleted successfully!");
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        session.setAttribute("errorDPMsg", "Pet deletion failed: " + e.getMessage());
	    }

	    return "redirect:/user/pet";
	}


	// Edit pet - show form
	@GetMapping("/edit/{id}")
	public String showEditPetForm(@PathVariable("id") int petId, Model model, HttpSession session,
			Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}

		String email = principal.getName();
		UserDtls user = userService.getUserByEmail(email);

		Pet pet = petService.getPetById(petId);
		if (pet == null || !pet.getOwner().getId().equals(user.getId())) {
			session.setAttribute("errorNoPetMsg", "not found or you don't have permission to edit this pet");
			return "redirect:/user/pet";
		}

		model.addAttribute("pet", pet);
		return "edit_pet"; // ชื่อไฟล์ HTML สำหรับฟอร์มแก้ไข
	}

	// Edit pet - handle form submission
	@PostMapping("/edit/{id}")
	public String updatePet(@RequestParam String name,
	                        @RequestParam String type,
	                        @RequestParam String breed,
	                      
	                        @RequestParam String description,
	                        @RequestParam("imagePet") MultipartFile imageFile,
	                        @PathVariable("id") int petId,
	                        HttpSession session,
	                        Principal principal) {
	    if (principal == null) {
	        return "redirect:/login";
	    }

	    String email = principal.getName();
	    UserDtls user = userService.getUserByEmail(email);

	    Pet existingPet = petService.getPetById(petId);
	    if (existingPet == null || !existingPet.getOwner().getId().equals(user.getId())) {
	        session.setAttribute("errorNoPetMsg", "Not found or you don't have permission to edit this pet");
	        return "redirect:/user/pet";
	    }

	    
	    try {
	        //String imageName = existingPet.getImagePet(); 
	        String imageUrl = commonUtil.getImageUrl(imageFile, BucketType.PETPROFILE.getId());

//	        if (!imageFile.isEmpty()) {
//	            String originalFilename = imageFile.getOriginalFilename();
//	            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
//
//	            if (!List.of("jpg", "jpeg", "png", "gif", "webp").contains(fileExtension)) {
//	                session.setAttribute("errorImagePMsg", "only image files are allowed (jpg, jpeg, png, gif, webp)");
//	                return "redirect:/user/pet";
//	            }
//
//	         
//	            if (imageName != null && !imageName.equals("default.jpg")) {
//	                String oldImagePath = System.getProperty("user.dir") + "/uploads/pet_img/" + imageName;
//	                Files.deleteIfExists(Paths.get(oldImagePath));
//	            }
//
////	          
////	            String fileName = UUID.randomUUID().toString() + "_" + originalFilename.replaceAll("\\s+", "_");
////
////	            String uploadDir = System.getProperty("user.dir") + "/uploads/pet_img/";
////	            File uploadFolder = new File(uploadDir);
////	            if (!uploadFolder.exists()) {
////	                uploadFolder.mkdirs();
////	            }
//
//	            Path filePath = Paths.get(uploadDir, fileName);
//	            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//	            imageName = fileName;
//	            
//	        }

	      
	        existingPet.setName(name);
	        existingPet.setType(type);
	        existingPet.setBreed(breed);
	       
	        existingPet.setDescription(description);
	        existingPet.setImagePet(imageUrl); 
	        existingPet.setOwner(user);

	        petService.updatePet(existingPet);
	        session.setAttribute("succUPMsg", "updated successfully!");
	        fileService.uploadFileS3(imageFile	, 4);
	    } catch (Exception e) {
	        
	        session.setAttribute("errorUPMsg", "updated failed "+ e.getMessage());
	    }

	    return "redirect:/user/pet";
	}

	
	
}