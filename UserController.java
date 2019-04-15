package org.cdlg.group.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.aspectj.util.FileUtil;
import org.cdlg.group.entity.Message;
import org.cdlg.group.entity.Space;
import org.cdlg.group.entity.User;
import org.cdlg.group.service.MessageService;
import org.cdlg.group.service.SpaceService;
import org.cdlg.group.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;


@Controller
public class UserController {

	
	@Autowired
	private UserService userService;
	@Autowired
	private MessageService messageService;
	@Autowired
	private SpaceService spaceService;
	
	
	public void setUserService(UserService userService) {
		this.userService = userService;
	}
	
	
	@RequestMapping("/doReadSpaceList")
	public String doReadSpaceList(HttpSession session,HttpServletRequest request) {
		User user=(User) session.getAttribute("user");
		if(user==null){
			return "redirect:/login.html";
		}
		List<Space> spaceList=spaceService.findSpaceById(user.getId());
		request.setAttribute("spaceList",spaceList);
		return "user_space";
	}
	
	@RequestMapping("/doUpSpaceImage")
	@ResponseBody
	public Object doUpSpaceImage(@RequestParam("file") MultipartFile file,HttpServletRequest request) {
		Map<String, Object> map = new HashMap<String, Object>();
		String path = request.getServletContext().getRealPath("/space_images/"+ file.getOriginalFilename());

		try {
			file.transferTo(new File(path ));
			map.put("imgurl", file.getOriginalFilename());
			map.put("msg", "上传成功！");
			
			String targePath="C:/Users/Acer/workspace1/cdlg_chatWHXT/WebContent/space_images/"+ file.getOriginalFilename();
			//第一个参数是源文件，第二个是目标文件
			FileUtil.copyFile(new File(path), new File(targePath));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
	    }
		return map;
	}
	
	@RequestMapping("/doAddSpace")
	public String doAddChatMessage(HttpSession session,Space space){
		User user=(User) session.getAttribute("user");
		if(user==null){
			return "redirect:/login.html";
		}
		space.setNickname(user.getNickname());
		space.setUserid(user.getId());
		spaceService.addSpace(space);
			
		return "redirect:/index.jsp";
	}
	
	
	
	
	@RequestMapping("/doAddChatMessage")
	@ResponseBody
	public Map<String, Object> doAddChatMessage(HttpSession session,Integer friendid,String message){
		User user=(User) session.getAttribute("user");
		if(user==null){
			return null;
		}
		Message mess=new Message();
		mess.setFromid(user.getId());
		mess.setToid(friendid);
		mess.setType(1);
		mess.setContent(message);
		messageService.addChatMessage(mess);
		
		Map<String, Object> resumap=new HashMap<String, Object>();
		
		resumap.put("success", "ok");
		
		return resumap;
	}
	
	@RequestMapping("/doUpdateCheckMessage")
	public String updateCheckMessage(HttpSession session,Integer friendid,boolean ispass){
		User user=(User) session.getAttribute("user");
		Message message=new Message();
		message.setFromid(friendid);
		message.setToid(user.getId());
		message.setType(2);
		messageService.updateMessageStatus(message, ispass);
	

		return "redirect:/doReadCheckMessage";
	}

	@RequestMapping("/doReadCheckMessage")
	public String readCheckMessage(HttpSession session,HttpServletRequest request){
		User user=(User) session.getAttribute("user");
		Message message=new Message();
		message.setToid(user.getId());
		
		List<Message> messageList=messageService.findCheckMessage(message);
		
		//临时代码
		
		request.setAttribute("messageList", messageList);
		request.setAttribute("messageCount", messageList.size());

		return "index";
	}
	


	@RequestMapping("/doFindFriendList")
	@ResponseBody
	public Map<String, Object> FindFriendList(HttpSession session){
		User user=(User) session.getAttribute("user");
		
		List<User> friendList=userService.findFriendList(user.getId());
		Map<String, Object> resumap=new HashMap<String, Object>();
		
		
		resumap.put("friendList", friendList);
		resumap.put("friendCount", friendList.size());
		/**
		 * 返回的内容会根据结果解析成JSON对象
		 * JSON对象的语法：{'friendCount':2,'friendList':[{'id':1,'nickname':'张三'},{'id':2,'nickname':'李白雪'}]}  []代表有很多个对象 {}单个对象
		 * {}
		 */
		
		return resumap;
	}
	
	
	@RequestMapping("/doReadChatMessage")
	@ResponseBody
	public Map<String, Object> doReadChatMessage(HttpSession session,Integer friendid){
		User user=(User) session.getAttribute("user");
		if(user==null){
			return null;
		}
		Message message=new Message();
		message.setFromid(friendid);
		message.setToid(user.getId());
		message.setType(1);
		List<Message> messageList=messageService.findChatMessage(message);
		
		User friend=userService.findUserById(friendid);
		Map<String, Object> resumap=new HashMap<String, Object>();
		
		resumap.put("user", user);
		resumap.put("friend", friend);
		resumap.put("messageList", messageList);

		/**
		 * 返回的内容会根据结果解析成JSON对象
		 * JSON对象的语法：{'friendCount':2,'friendList':[{'id':1,'nickname':'张三'},{'id':2,'nickname':'李白雪'}]}  []代表有很多个对象 {}单个对象
		 * {}
		 */
		
		return resumap;
	}
	
	
	/**
	 * 登录具体实现
	 * 
	 * 接收参数，将参数写在形参
	 * @throws UnknownHostException 
	 */
	@RequestMapping("/doLogin")
	public String login(String username,String password,HttpSession session) throws UnknownHostException{
		boolean b=this.userService.login(username, password);
	
		if(b){
			InetAddress inetAddress=Inet4Address.getLocalHost();
			String string=inetAddress.getHostAddress();
	
			session.setAttribute("username", username);
			User user=this.userService.getUserByUserid(username);
			user.setType(1);
			user.setIdAddress(string);
			session.setAttribute("user", user);
			userService.addLog(user);
			return "redirect:/doReadCheckMessage";
			}else {
				return "redirect:/login.html";
				}
		}
	/**
	 * 
		User u=new User();
		u.setUserid(username);
		u.setPassword(password);
		
		userService.register(u);
		return "redirect:/login.html";
	*/
	@RequestMapping("/doRegister")
	public String register(String username,String password,HttpSession session){
		User user=new User();
		user.setUserid(username);
		user.setPassword(password);
		boolean b=this.userService.register(user);
		if(b){
			
			return "register_success";
			
		}else {
				return "redirect:/login.html";
			}
		}
	
	@RequestMapping("/getUserInfo")
	public String getUserInfo(HttpSession session,HttpServletRequest request){
		String userid=(String) session.getAttribute("username");
		User user = this.userService.getUserByUserid(userid);
		
		request.setAttribute("user", user);
				
		return "information";
	}
	
	@RequestMapping("/logOut")
	public String logOut(HttpSession session){
		Date date=new Date();
		SimpleDateFormat dateFormat=new SimpleDateFormat("YYYY-MM-dd");
		String stringdate=dateFormat.format(date.getTime());
		User user=new User();
		try {
			user.setLasttime(dateFormat.parse(stringdate));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String userid=(String) session.getAttribute("username");
		user.setUserid(userid);
		
		this.userService.logOutUpdate(user);
		return "redirect:/login.html";
	}
	
	@RequestMapping("/infoSeting")
	public String infoSeting(HttpSession session,HttpServletRequest request){
		String userid=(String) session.getAttribute("username");
		User user = this.userService.getUserByUserid(userid);
		
		request.setAttribute("user", user);
				
		return "info-setting";
	}
	
	@RequestMapping("/setInfo")
	public String  modifyInfomation(String nickname,Integer checktype,String sex,Integer age,String profile,HttpSession session){
		
		User user=new User();
		String userid = (String) session.getAttribute("username");
		user.setUserid(userid);
		user.setNickname(nickname);
		user.setChecktype(checktype);
		user.setSex(sex);
		user.setAge(age);
		user.setProfile(profile);
		
		boolean b=this.userService.updateInfomation(user);
		
		return "index";  
	}
	
	/**
	 * 接收文件上传
	 * @return
	 */
	@RequestMapping("/upload")
	public String upload(@RequestParam("file")MultipartFile file,HttpSession session){
		//将file写入服务器的某个目录下
//		先获取文件名称
		String filename = file.getOriginalFilename();
		//通过io流将文件写入D:\apache-tomcat-8.0.53\webapps\headimgaes
		InputStream input=null;
		OutputStream output=null;
		try {
			 input=file.getInputStream();
			 output=new FileOutputStream("D:\\tools\\apache-tomcat-8.0.53\\webapps\\headimgaes\\"+filename);
			IOUtils.copy(input, output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			try {
				input.close();
				output.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		/*
		 * 将上传的文件名称filename值保存到chatuser表的当前用户记录中
		 * 发出update语句，根据userid去修改profielhead
		 */
		User user=new User();
		user.setProfilehead(filename);
		String userid = (String) session.getAttribute("username");
		user.setUserid(userid);


		
		this.userService.byUseridUpdateProfilehead(user);
		
		
		return "info-setting";
		
	}
	/**通過userid与oldpassword查询信息并进行修改密码
	 * @return 
	 * 
	 */
	
	@RequestMapping("/pass")
	public String modifyPassword(HttpSession session,String oldpass,String newpass,String newpass1){
		String userid=(String) session.getAttribute("username");
		User user = this.userService.getUserByUserid(userid);
		
		if(oldpass.equals(user.getPassword()) && (newpass.equals(newpass1))){
			user.setPassword(newpass);
			user.setUserid(userid);
			boolean a=this.userService.updatePassword(user);
			if (a) {
				return "redirect:/login.html";
				
			}else {
				return "info-setting";
			}
		}
		return "info-setting";
	}
	/**
	 * 查询可以添加的好友列表
	 */
	
	@RequestMapping("/doNotFriend")
	public String findNotFriend(HttpSession session,HttpServletRequest request,String userid,String nickname,Integer nowpage){
		if(nowpage==null||nowpage==0){
			nowpage=1;
		}
		User user = (User) session.getAttribute("user");
				
		if(user==null){
			return "redirect:/login.html";
		}
		Map<String, Object> map=new HashMap<String,Object>();
		map.put("id", user.getId());
		map.put("userid", userid);
		map.put("nickname", nickname);
		map.put("startindex", (nowpage-1)*3);

		List<User> userList=userService.findNotFriendUser(map);
		int pagecount=userService.findNotFriendPages(map);
		
		int backpage=nowpage-1;
		if(backpage<1){
			backpage=1;
		}
		int nextpage=nowpage+1;
		if(nextpage>pagecount)
			nextpage=pagecount;
		
		request.setAttribute("nextpage", nextpage);
		request.setAttribute("backpage", backpage);
		request.setAttribute("nowpage", nowpage);
		request.setAttribute("userList", userList);
		request.setAttribute("pagecount", pagecount);

		//保存搜索条件，为页面实现搜索条件回显提供数据
		request.setAttribute("userid", userid);
		request.setAttribute("nickname", nickname);

				
		return "friend";
	}

	@RequestMapping("/doAddFriend")
	public String addFriend(HttpSession session,Integer friendid){
		User user=(User) session.getAttribute("user");
		if(user==null){
			return "redirect:/login.html";
		}
		int num=userService.addFriend(user.getId(), friendid);
		if(num==0){
			session.setAttribute("msg", "已经发送了请求消息");
		}else {
			session.setAttribute("msg", "添加好友成功，请刷新好友列表");
		}

		return "redirect:/doNotFriend";
	}
	
	@RequestMapping("/log")
	public String log(){
		return "log";
	}
}
