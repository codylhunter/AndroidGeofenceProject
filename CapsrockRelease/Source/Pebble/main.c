/*
 * Pebble Application
 * Handles all the Pebble functionalities. Keeps track of Geofence activities and synchronized informations
 * with the Android Application.
 *
 * Team Capsrock : Richard Bae, Chris Beichler, Cody Hunter, Taylor Woods
 */

#include <pebble.h>

int CMD_KEY_RCV = 0;
int CMD_KEY_SEND = 1;

char* CMD_SELECT = "Select";
char* CMD_UP = "Up";
char* CMD_DOWN = "Down";

Window* window;
TextLayer* location_layer;
TextLayer* work_layer;
TextLayer* timer_layer;
TextLayer* break_layer;
TextLayer* work_x;
TextLayer* break_x;

Window* notif_window;
TextLayer* notif_layer;
TextLayer* yes_layer;
TextLayer* no_layer;
ActionBarLayer* notif_action_bar;

char* global_msg;
char* global_str;
char str_buffer[16];

int on_work = 0;
int on_break = 0;

bool geo_enter = false;
bool geo_exit = false;
static bool started_work = false;

//Timer global variable
static double elapsed_time = 0;
static bool started = false;
static AppTimer* update_timer = NULL;
static double start_time = 0;
static double pause_time = 0;

void update_stopwatch(void);
static void send_char_cmd(char* cmd);

int text_layer_length;

void up_single_click_handler(ClickRecognizerRef recognizer, void *context);
void notif_config_provider(Window* window);

int time_received = 0;

/* Function: my_atoi
 * Purpose: Converts character string into a long long int
 */
long long int my_atoi(char* num)
 {
     long long int res = 0;
	for(int i = 0; num[i] != '\0'; ++i)
		res = res * 10 + num[i] - '0';
	
	return res;
 }

//---------------------------------- Timer Functions ---------------------------------
/* Function: timeparse
 * Purpose: Parse time in seconds into "hr:min:sec" format
 */
char* timeparse(double militime){
   // Convert time in milisecond into hour:minute:second string format
   static char curTime[] = "00:00:00";

   // Now convert to hours/minutes/seconds.
   int seconds = (int)militime % 60;
   int minutes = (int)militime / 60 % 60;
   int hours = (int)(militime / 3600) % 24;

   snprintf(curTime, 10, "%02d:%02d:%02d", hours, minutes, seconds);

   return curTime;
}

/* 
 * Function: new_time_ms
 * Purpose: Gets current local time in seconds on Pebble in 24 hour mode since today's midnight
 */
double new_time_ms() {
   time_t seconds;
   struct tm *rtn_time;
   seconds = time(NULL);
   rtn_time = localtime(&seconds);
   return (double)(rtn_time->tm_hour * 3600 + rtn_time->tm_min * 60 + rtn_time->tm_sec);
}

/*
 * Function: handle_timer
 * Purpose: Gets current time and substract the current time from the time when timer was started
 * to keep track of a timer. Then it parses the time in seconds into "hr:min:sec" and updates the
 * Pebble screen
 */  
void handle_timer(void* data) {
   // updates clock every second
   if(started) {
      double cur_time = new_time_ms();
      elapsed_time = cur_time - start_time;
      update_timer = app_timer_register(100, handle_timer, NULL);
   }
   update_stopwatch();
}

/*
 * Function: stop_stopwatch
 * Purpose: Stops the timer and resets appropriate fields.
 */
void stop_stopwatch() {
   //Stop stopwatch
   started = false;
   start_time = 0;
   elapsed_time = 0;
   update_stopwatch();
   if(update_timer != NULL) {
      app_timer_cancel(update_timer);
      update_timer = NULL;
   }
}

/*
 * Function: start_stopwatch
 * Purpose: Marks current time and runs update timer to start the timer
 */
void start_stopwatch() {
   started = true;
   if(start_time == 0) {
      start_time = new_time_ms();
   }
   
   update_timer = app_timer_register(100, handle_timer, NULL);
}

/*
 * Function: update_stopwatch
 * Purpose: Parses the timer into "hr:min:sec" format and displays it on the Pebble screen.
 */
void update_stopwatch() {
   static char big_time[] = "00:00:00";

   // Now convert to hours/minutes/seconds.
   int seconds = (int)elapsed_time % 60;
   int minutes = (int)elapsed_time / 60 % 60;
   int hours = (int)elapsed_time / 3600;

   // We can't fit three digit hours, so stop timing here.
   if(hours > 99) {
      stop_stopwatch();
      return;
   }

   snprintf(big_time, 10, "%02d:%02d:%02d", hours, minutes, seconds);

   // Now draw the strings.
   text_layer_set_text(timer_layer, big_time);
}



//----------------------- Text Message Generator Function --------------------------
/*
 * Function: disp_msg
 * Purpose: displays a message onto a designated text layer.
 */
void disp_msg(TextLayer* layer, char* msg){
  text_layer_set_text(layer, msg);
  layer_mark_dirty((Layer*) layer);
}

//----------------------- Initializing text layer ----------------------------------
/*
 * Function: init_layer
 * Purpose: initialize the main window and all the necessary layers to display various informations
 * on Pebble
 */
void init_layer(){
	
	location_layer = text_layer_create(GRect(0, 68, 144, 28));
	work_layer = text_layer_create(GRect(85, 0, 50, 72));
	timer_layer = text_layer_create(GRect(0, 40, 144, 28));
	break_layer = text_layer_create(GRect(85, 100, 50, 72));
	  
	work_x = text_layer_create(GRect(130, 10, 14, 72));
	break_x = text_layer_create(GRect(130, 110, 14, 72));
	
	text_layer_set_text_alignment(location_layer, GTextAlignmentCenter);
	text_layer_set_text_alignment(work_layer, GTextAlignmentLeft);
	text_layer_set_text_alignment(timer_layer, GTextAlignmentCenter);
	text_layer_set_text_alignment(break_layer, GTextAlignmentLeft);
	text_layer_set_text_alignment(work_x, GTextAlignmentLeft);
	text_layer_set_text_alignment(break_x, GTextAlignmentLeft);
	
	text_layer_set_font(location_layer, fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD));
	text_layer_set_font(work_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
	text_layer_set_font(timer_layer, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
	text_layer_set_font(break_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
	text_layer_set_font(work_x, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD));
	text_layer_set_font(break_x, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD));
		
	layer_add_child((Layer*)window, (Layer*)location_layer);
	layer_add_child((Layer*)window, (Layer*)work_layer);
	layer_add_child((Layer*)window, (Layer*)timer_layer);
	layer_add_child((Layer*)window, (Layer*)break_layer);
	layer_add_child((Layer*)window, (Layer*)work_x);
	layer_add_child((Layer*)window, (Layer*)break_x);
	
	disp_msg(location_layer, "No Location");
	disp_msg(work_layer, "Work Time");
	text_layer_set_text(timer_layer, "00:00:00");
	disp_msg(break_layer, "Break Time");
	disp_msg(work_x, "X");
	disp_msg(break_x, "X");
	
	text_layer_length = text_layer_get_content_size(location_layer).w;
	
	layer_set_hidden((Layer*)work_x, true);
	layer_set_hidden((Layer*)break_x, true);
	
	
}

/*
 * Function: clock_yes_click_handler
 * Purpose: handles when user clicks 'yes' on notification window after user enters or exits
 * geofence locations
 */
void clock_yes_click_handler(ClickRecognizerRef recognizer, void *context) {
	const bool animated = true;
	window_stack_pop(animated);
	if(geo_enter){
		geo_exit = false;
		send_char_cmd("12345678 Work");
		up_single_click_handler(recognizer, context);
	}
	if(geo_exit){
		geo_enter = false;
		send_char_cmd("stop");
		
		on_work = 0;
		on_break = 0;
		layer_set_hidden((Layer*)work_x, true);
		text_layer_set_font(work_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
		layer_set_hidden((Layer*)break_x, true);
		text_layer_set_font(break_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
		stop_stopwatch();
		started_work = false;
		start_time = 0;
		elapsed_time = 0;
		update_stopwatch();
	}

}

/*
 * Fucntion: clock_no_click_handler
 * Purpose: handles when user clicks 'no' button on notification window
 */
void clock_no_click_handler(ClickRecognizerRef recognizer, void *context) {
	//Pop the window to go back to app
	const bool animated = true;
	window_stack_pop(animated);
	send_char_cmd("dismiss");
}

/*
 * Function: notif_action_click_config_provider
 * Purpose: to install handlers for 'yes' and 'no' button for notification screen
 */
void notif_action_click_config_provider(void *context) {
	window_single_click_subscribe(BUTTON_ID_UP, (ClickHandler) clock_yes_click_handler);
	window_single_click_subscribe(BUTTON_ID_DOWN, (ClickHandler) clock_no_click_handler);
}

/*
 * Function: init_notif_layer
 * Purpose: initialize notification window and its layers to display message for entering or
 * exiting geofence locations
 */
void init_notif_layer(char* status, char* loc){
	notif_layer = text_layer_create(GRect(0, 18, 122, 120));
	yes_layer = text_layer_create(GRect(83, 0, 50, 18));
	no_layer = text_layer_create(GRect(83, 120, 50, 18));
	
	text_layer_set_text_alignment(notif_layer, GTextAlignmentCenter);
	text_layer_set_text_alignment(yes_layer, GTextAlignmentCenter);
	text_layer_set_text_alignment(no_layer, GTextAlignmentCenter);
	
	text_layer_set_font(notif_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD));
	text_layer_set_font(yes_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
	text_layer_set_font(no_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));

	notif_action_bar = action_bar_layer_create();

	action_bar_layer_add_to_window(notif_action_bar, notif_window);
	action_bar_layer_set_click_config_provider(notif_action_bar, notif_action_click_config_provider);

	action_bar_layer_set_icon(notif_action_bar, BUTTON_ID_UP, NULL);
	action_bar_layer_set_icon(notif_action_bar, BUTTON_ID_DOWN, NULL);
	
	layer_add_child((Layer*)notif_window, (Layer*)notif_layer);
	layer_add_child((Layer*)notif_window, (Layer*)yes_layer);
	layer_add_child((Layer*)notif_window, (Layer*)no_layer);
	
	layer_add_child((Layer*)notif_window, (Layer*)notif_action_bar);
	
	disp_msg(yes_layer, "Yes");
	disp_msg(no_layer, "No");

	char* notif_str = malloc(200 * sizeof(char));
	memset(notif_str, '\0', sizeof(notif_str));
	strcpy(notif_str, "You are ");
	if(!strncmp(status, "enter", 5)){
		geo_enter = true;
		geo_exit = false;
		strcat(notif_str, "enter");
	}
	else{
		geo_enter = false;
		geo_exit = true;
		strcat(notif_str, "exit");
	}
	strcat(notif_str, "ing ");
	strcat(notif_str, loc);
	strcat(notif_str, "!\n Would you like to clock ");
	if(!strncmp(status, "enter", 5)){
		strcat(notif_str, "in?");
	}
	else{
		strcat(notif_str, "out?");
	}
	
	disp_msg(notif_layer, notif_str);
	
}

//---------------------- Sending String Command to Phone ----------------------------
/*
 * Function: send_char_cmd
 * Purpose: sends strings to Android application
 */
static void send_char_cmd(char* cmd) {
    
  DictionaryIterator *iter;

  Tuplet time_value = TupletCString(CMD_KEY_SEND, cmd);
  
  app_message_outbox_begin(&iter);
  
  if (iter == NULL)
    return;
  
  dict_write_tuplet(iter, &time_value);
  dict_write_end(iter);
  
  app_message_outbox_send();
}

//--------------------------- Click Handlers -------------------------------------
/*
 * Function: select_single_click_handler
 * Purpose: handles when user presses on 'middle' button on Pebble when user is in main window
 * It only vibrates with no other functionalities
 */
void select_single_click_handler(ClickRecognizerRef recognizer, void *context) {
  //Single Click recognized
  vibes_short_pulse();
}

/*
 * Function: on_work_handler
 * Purpose: handles functions when work time button is pressed. It enables timer to keep track of work time, 
 * and sets necessary variables.
 */
void on_work_handler(long long int pausetime){
	started_work = true;
	if(on_work == 0){ //Start Timer. Counting Timer from Scratch
		on_work = 1;

		layer_set_hidden((Layer*)work_x, false);
		text_layer_set_font(work_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD));
	
		on_break = 0;
		layer_set_hidden((Layer*)break_x, true);
		text_layer_set_font(break_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
		stop_stopwatch();
	
		if (!pausetime){
			start_time = 0;
			elapsed_time = 0;
}

	   
	else{
		start_time = pausetime;
		elapsed_time = (long long int)new_time_ms() - start_time;
	}
	   
	update_stopwatch();
     start_stopwatch();
   }
   else{  //Stop Timer
	  on_work = 0;
	  started_work = false;
	  layer_set_hidden((Layer*)work_x, true);
	  text_layer_set_font(work_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
	  stop_stopwatch();
	  
   }
}

/*
 * Function: up_single_click_handler
 * Purpose: handles when user presses on 'top' button on Pebble when user is in main window
 * It triggers on_work_handler function
 */
void up_single_click_handler(ClickRecognizerRef recognizer, void *context) {
   int temp_work = on_work;
   on_work_handler(0);

   if(temp_work == 0){
       strcpy(str_buffer, timeparse(pause_time));
       char* sendTimeToPhone = strcat(str_buffer, " Work");
       send_char_cmd(sendTimeToPhone);
   }
   else if(temp_work == 1){
      strcpy(str_buffer, timeparse(pause_time));
       char* sendTimeToPhone = strcat(str_buffer, " stop");
       send_char_cmd(sendTimeToPhone);
   }
}

/*
 * Function: on_break_handler
 * Purpose: handles functions when break time button is pressed. It enables timer to keep track of break time, 
 * and sets necessary variables.
 */
void on_break_handler(long long int pausetime){
   
   if(on_break == 0){
 	  on_break = 1;
      layer_set_hidden((Layer*)break_x, false);
	  text_layer_set_font(break_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD));

	  on_work = 0;
	  layer_set_hidden((Layer*)work_x, true);
	  text_layer_set_font(work_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
	   
      stop_stopwatch();
      if (!pausetime){
           start_time = 0;
           elapsed_time = 0;
        }
	   
	   else{
		   start_time = pausetime;
		   elapsed_time = (long long int)new_time_ms() - start_time;
	   }
      update_stopwatch();
	  start_stopwatch();
      
   }
   else{
	  on_break = 0;
	  started_work = false;
	  layer_set_hidden((Layer*)break_x, true);
	  text_layer_set_font(break_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
	  stop_stopwatch();
   }
}

/*
 * Function: down_single_click_handler
 * Purpose: handles when user presses on 'bottom' button on Pebble when user is in main window
 * It triggers on_break_handler function
 */
void down_single_click_handler(ClickRecognizerRef recognizer, void *context) {
	if (started_work){

	int temp_break = on_break;
	
	   on_break_handler(0);
	
	   if(temp_break == 0){
		  strcpy(str_buffer, timeparse(pause_time));
		  char* sendTimeToPhone = strcat(str_buffer, " Break");
		  send_char_cmd(sendTimeToPhone);
	   }
	   else if (temp_break == 1){
		  strcpy(str_buffer, timeparse(pause_time));
		  char* sendTimeToPhone = strcat(str_buffer, " stop");
		  send_char_cmd(sendTimeToPhone);
	   }
	}
}
//---------------------- AppMessage Handlers --------------------------------------
/*
 * Function: my_out_sent_handler
 * Purpose: gets called when a message to Android Application was sent successfully
 * Unused in our application
 */
void my_out_sent_handler(DictionaryIterator *sent, void *context) {
  // outgoing message was delivered
}

/*
 * Function: my_out_fail_handler
 * Purpose: gets called when a message to Android Application didn't get sent.
 * Unused in our application
 */
void my_out_fail_handler(DictionaryIterator *failed, AppMessageResult reason, void *context) {
  // outgoing message failed
}

/*
 * Function: my_in_rcv_handler
 * Purpose: gets called when a message is received from Android Application successfully
 * Used to check what kind of message is sent from Android Application and process them properly to
 * call corresponding functions for the Pebble timer
 */
void my_in_rcv_handler(DictionaryIterator *received, void *context){
	Tuple* check_tuple = dict_find(received, CMD_KEY_RCV);
	char* check = check_tuple->value->cstring;
	char* loc_str = NULL;
	char* status = NULL;
	
	if (!strcmp(check, "dismiss")){
		if(notif_window != NULL && notif_window == window_stack_get_top_window())
			window_stack_pop(true);
	}
	
	else if (!strncmp(check, "stop", 4)){
		 on_work = 0;
		 on_break = 0;
		started_work = false;
		 layer_set_hidden((Layer*)work_x, true);
		 text_layer_set_font(work_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
		 layer_set_hidden((Layer*)break_x, true);
		 text_layer_set_font(break_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18));
		 stop_stopwatch();
		 start_time = 0;
		 elapsed_time = 0;
		 update_stopwatch();
	}
	
	else{
		size_t count;
		for (count = 0; count < strlen(check) - 1; count++){
			if(check[count] == ' '){
				status = malloc(count * sizeof(char));
				memset(status, '\0', sizeof(status));
				memcpy(status, check, count);
				loc_str = (char*)&check[count+1];
				break;
			}
		}
		if(status != NULL){
			if(!strncmp(status, "stimework", 9)){
				if (notif_window != NULL && notif_window == window_stack_get_top_window()){
					window_stack_pop(true);
				}
			// Work time log start
				on_work_handler(my_atoi(loc_str));
			}
			
			else if(!strncmp(status, "stimebreak", 10)){
				on_break_handler(my_atoi(loc_str));
			}
			
			else if(!strncmp(status, "timebreak", 9) && !time_received){
				time_received = 1;
				long long int ret = my_atoi(loc_str);
			
				on_break_handler(ret);
			}
			else if(!strncmp(status, "timework", 8) && !time_received){
				time_received = 1;
				long long int ret = my_atoi(loc_str);
				
				on_work_handler(ret);
			}
			else if(!strncmp(status, "timenone", 8)){
				started_work = false;
			}
			
			else if(!strncmp(status, "enter", 5)){
				if (loc_str != NULL){
                    free(global_str);
                    global_str = malloc(strlen(loc_str));
                    memset(global_str, 0, sizeof(global_str));
                    memcpy(global_str, loc_str, strlen(loc_str));
                }
				window_stack_push(notif_window, true);
				init_notif_layer(status, global_str);
				disp_msg(location_layer, global_str);
			}
			
			else if(!strncmp(status, "exit", 4)){
				if (loc_str != NULL){
                    free(global_str);
                    global_str = malloc(strlen(loc_str));
                    memset(global_str, 0, sizeof(global_str));
                    memcpy(global_str, loc_str, strlen(loc_str));
                }
				window_stack_push(notif_window, true);
				init_notif_layer(status, global_str);
				disp_msg(location_layer, global_str);
			}
			
			else{
				//Set location and display
				disp_msg(location_layer, global_str);
				//Start the notification window
				if (notif_window != NULL && notif_window == window_stack_get_top_window()){
					window_stack_pop(true);
				}
			}
		}
	}
	vibes_short_pulse();
}

/*
 * Function: my_in_drp_handler
 * Purpose: gets called when Pebble fails to get the message from Android Application
 * Unused in our application
 */
void my_in_drp_handler(AppMessageResult reason, void *context) {
    
}

//------------------ Button Config Provider Function ---------------------------------
/*
 * Function: config_provider
 * Purpose: installs button functionalities to Pebble's main window
 */
void config_provider(Window* window) {
  // single click / repeat-on-hold config:
  window_single_click_subscribe(BUTTON_ID_SELECT, select_single_click_handler);
  window_single_click_subscribe(BUTTON_ID_UP, up_single_click_handler);
  window_single_click_subscribe(BUTTON_ID_DOWN, down_single_click_handler);

}

//------------------------- Initializing App -------------------------------------
/*
 * Function: hadnle_init
 * Purpose: create necessary windows and set up layers for the main window to display various
 * informations
 */
void handle_init(void) {

  window = window_create();
  notif_window = window_create();

  window_stack_push(window, true);
  window_set_click_config_provider(window, (ClickConfigProvider) config_provider);
  init_layer();

  app_message_register_inbox_received(my_in_rcv_handler);
  app_message_register_inbox_dropped(my_in_drp_handler);
  app_message_register_outbox_sent(my_out_sent_handler);
  app_message_register_outbox_failed(my_out_fail_handler);

  const uint32_t inbound_size = 64;
  const uint32_t outbound_size = 64;
  app_message_open(inbound_size, outbound_size);


}

//-------------------------- Deinitialize App ------------------------------------
/*
 * Function: handle_deinit
 * Purpose: destroys all the windows and layers used and cleans up the application before quitting
 */
void handle_deinit(void){
	
  text_layer_destroy(location_layer);
  text_layer_destroy(work_layer);
  text_layer_destroy(timer_layer);
  text_layer_destroy(break_layer);
  
  text_layer_destroy(work_x);
  text_layer_destroy(break_x);

  text_layer_destroy(notif_layer);
  text_layer_destroy(yes_layer);
  text_layer_destroy(no_layer);
  
  window_destroy(notif_window);
  window_destroy(window);
}

//-------------------------- Main Function ----------------------------------------
/*
 * Function: main
 * Purpose: main application function
 */
int main (void){
  handle_init();
  send_char_cmd("request_time");
  app_event_loop();
  handle_deinit();
}