package com.israel.hebrewcalendar;

import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import android.icu.util.HebrewCalendar;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.*;

public class HebrewCalendarFragment extends Fragment {
    private CalendarTableView calendarView;
    private FrameLayout calendarContainer;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private final Map<String,List<CalendarEntry>> calendarEntries=new HashMap<>();

    public HebrewCalendarFragment(){super(R.layout.fragment_calendar);}

    @Override public void onResume(){
        super.onResume();
        if(mAuth!=null) loadCalendarEntries();
    }

    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle b){
        super.onViewCreated(v,b);
        mAuth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();
        calendarContainer=v.findViewById(R.id.calendarContainer);
        Spinner spinner=v.findViewById(R.id.yearSpinner);
        Button logout=v.findViewById(R.id.logoutButton);

        logout.setOnClickListener(x->{
            mAuth.signOut();
            Intent i=new Intent(requireContext(),LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        calendarView=new CalendarTableView(requireContext());
        calendarContainer.removeAllViews();
        calendarContainer.addView(calendarView,new FrameLayout.LayoutParams(-1,-1));

        Integer[] years=new Integer[21];
        for(int i=0;i<21;i++) years[i]=2020+i;

        ArrayAdapter<Integer> adapter=new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(6,false);

        calendarView.selectedYear=2026;

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                Integer y=(Integer)p.getItemAtPosition(pos);
                if(y==null)return;
                calendarView.selectedYear=y;
                calendarView.invalidate();
                if(y==2026)calendarView.post(calendarView::focusOnToday);
            }
            @Override public void onNothingSelected(AdapterView<?> p){}
        });

        loadCalendarEntries();
        calendarView.post(()->{
            calendarView.selectedYear=2026;
            calendarView.focusOnToday();
        });
    }

    private void loadCalendarEntries(){
        FirebaseUser u=mAuth.getCurrentUser();
        if(u==null)return;

        db.collection("users").document(u.getUid()).collection("calendarEntries").get()
                .addOnSuccessListener(s->{
                    calendarEntries.clear();
                    for(DocumentSnapshot d:s){
                        String start=d.getString("startDate");
                        String end=d.getString("endDate");

                        if(empty(start)&&empty(end)){
                            start=convertOldDateToNewFormat(d.getString("gregorianDate"));
                            end=start;
                        }
                        if(empty(start)||empty(end))continue;

                        CalendarEntry e=new CalendarEntry();
                        e.id=d.getId();
                        e.startDate=start;
                        e.endDate=end;
                        e.hebrewDate=d.getString("hebrewDate");
                        e.type=d.getString("type");
                        e.title=d.getString("title");
                        e.startTime=d.getString("startTime");
                        e.endTime=d.getString("endTime");
                        e.allDay=Boolean.TRUE.equals(d.getBoolean("allDay"));
                        e.location=d.getString("location");
                        e.link=d.getString("link");
                        e.reminder=d.getString("reminder");
                        e.description=d.getString("description");
                        addEntryToDateRange(e);
                    }
                    if(calendarView!=null)calendarView.invalidate();
                })
                .addOnFailureListener(e->{
                    if(isAdded())Toast.makeText(requireContext(),
                            "טעינת הנתונים נכשלה: "+e.getMessage(),Toast.LENGTH_LONG).show();
                });
    }

    private boolean empty(String s){return s==null||s.trim().isEmpty();}


    private void addEntryToDateRange(CalendarEntry e){
        if(e==null||empty(e.startDate)||empty(e.endDate))return;
        Calendar s=parseDate(e.startDate),end=parseDate(e.endDate);
        if(s==null||end==null)return;
        normalizeDate(s); normalizeDate(end);
        if(end.before(s))return;

        for(Calendar c=(Calendar)s.clone();!c.after(end);c.add(Calendar.DAY_OF_MONTH,1))
            calendarEntries.computeIfAbsent(formatDateKey(c),k->new ArrayList<>()).add(e);
    }

    private String convertOldDateToNewFormat(String value){
        if(empty(value))return null;
        try{
            java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("dd/MM/yyyy",Locale.US);
            f.setLenient(false);
            Date d=f.parse(value);
            return d==null?null:new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(d);
        }catch(Exception e){return null;}
    }

    private Calendar parseDate(String value){
        if(empty(value))return null;
        try{
            java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US);
            f.setLenient(false);
            Date d=f.parse(value);
            if(d==null)return null;
            Calendar c=Calendar.getInstance();
            c.setTime(d);
            return c;
        }catch(Exception e){return null;}
    }

    private void normalizeDate(Calendar c){
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
    }

    private String formatDateKey(Calendar c){
        return String.format(Locale.US,"%04d-%02d-%02d",
                c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));
    }

    public static class CalendarEntry{
        public String id,startDate,endDate,hebrewDate,type,title,startTime,endTime,location,link,reminder,description;
        public boolean allDay;
    }

    private class CalendarTableView extends View {
        private final float density;
        private final int DAYS=37,DAY_WIDTH,ROW_HEIGHT,MONTH_WIDTH,YEAR_WIDTH,MONTH_HEIGHT,MONTHS=13,TABLE_WIDTH,TABLE_HEIGHT;
        private int selectedYear=2026;
        private final int START_MONTH=Calendar.DECEMBER;
        private float scaleFactor=1f,panX,panY,lastTouchX,lastTouchY,downX,downY;
        private final float MAX_SCALE=1.3f;
        private boolean moved;
        private static final int TOUCH_SLOP=12;

        private final int COLOR_YELLOW=Color.rgb(255,230,153);
        private final int COLOR_BLUE=Color.rgb(189,215,238);
        private final int COLOR_PINK=Color.rgb(255,153,255);
        private final int COLOR_GREEN=Color.rgb(198,224,180);
        private final int COLOR_GRAY=Color.rgb(174,170,170);
        private final int COLOR_EVENT=Color.rgb(52,120,246);
        private final int COLOR_INCOME=Color.rgb(46,160,67);
        private final int COLOR_EXPENSE=Color.rgb(220,53,69);
        private final int COLOR_NOTE=Color.rgb(245,180,40);
        private final int COLOR_TODAY=Color.rgb(25,118,210);
        private final int COLOR_TODAY_BACKGROUND=Color.rgb(225,240,255);

        private final Paint fill=new Paint(1),line=new Paint(1),text=new Paint(1),border=new Paint(1),todayPaint=new Paint(1);
        private final String[] englishMonths={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        private final String[] hebrewWeekDays={"א","ב","ג","ד","ה","ו","ש"};
        private final ScaleGestureDetector scaleDetector;

        CalendarTableView(Context c){
            super(c);
            density=getResources().getDisplayMetrics().density;
            DAY_WIDTH=dp(55); ROW_HEIGHT=dp(34); MONTH_WIDTH=dp(120); YEAR_WIDTH=dp(85);
            MONTH_HEIGHT=ROW_HEIGHT*7;
            TABLE_WIDTH=DAYS*DAY_WIDTH+MONTH_WIDTH+YEAR_WIDTH;
            TABLE_HEIGHT=MONTHS*MONTH_HEIGHT;

            fill.setStyle(Paint.Style.FILL);
            line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(dp(1)); line.setColor(Color.DKGRAY);
            text.setColor(Color.BLACK); text.setTextSize(dp(12)); text.setAntiAlias(true);
            border.setStyle(Paint.Style.STROKE); border.setColor(Color.BLACK);
            todayPaint.setStyle(Paint.Style.STROKE); todayPaint.setColor(COLOR_TODAY); todayPaint.setStrokeWidth(dp(3));
            setBackgroundColor(Color.WHITE); setClickable(true);
            scaleDetector=new ScaleGestureDetector(c,new ScaleListener());
        }

        private int dp(int n){return(int)(n*density+.5f);}
        private float frozenWidth(){return(MONTH_WIDTH+YEAR_WIDTH)*scaleFactor;}
        private float scrollableWidth(){return DAYS*DAY_WIDTH*scaleFactor;}

        private float minScale(){
            if(getWidth()<=0||getHeight()<=0)return .25f;
            return Math.min(getWidth()/(float)TABLE_WIDTH,getHeight()/(float)TABLE_HEIGHT);
        }

        @Override protected void onSizeChanged(int w,int h,int ow,int oh){
            super.onSizeChanged(w,h,ow,oh);
            if(ow==0&&oh==0){scaleFactor=minScale();clampPan();}
        }

        private void focusOnToday(){
            Calendar now=Calendar.getInstance();
            int y=now.get(Calendar.YEAR),m=now.get(Calendar.MONTH),d=now.get(Calendar.DAY_OF_MONTH);
            Calendar start=Calendar.getInstance();
            start.set(selectedYear - 1,START_MONTH,1);
            Calendar end=(Calendar)start.clone();
            end.add(Calendar.MONTH,MONTHS);end.add(Calendar.DAY_OF_MONTH,-1);

            Calendar today=Calendar.getInstance();
            today.set(y,m,d);
            if(today.before(start)||today.after(end)){invalidate();return;}

            int monthIndex=0;
            Calendar ms=(Calendar)start.clone();
            while(monthIndex<MONTHS){
                if(ms.get(Calendar.YEAR)==y&&ms.get(Calendar.MONTH)==m)break;
                ms.add(Calendar.MONTH,1);monthIndex++;
            }
            if(monthIndex>=MONTHS){invalidate();return;}

            Calendar first=Calendar.getInstance();
            first.set(y,m,1);
            int empty=first.get(Calendar.DAY_OF_WEEK)-1;
            int pos=empty+d-1;
            if(pos<0||pos>=DAYS){invalidate();return;}

            float dayX=(DAYS-pos-1)*DAY_WIDTH,dayY=monthIndex*MONTH_HEIGHT;
            scaleFactor=Math.min(MAX_SCALE,Math.max(minScale(),.50f));
            float frozen=frozenWidth(),viewport=getWidth()-frozen;
            panX=viewport/2f-(dayX+DAY_WIDTH/2f)*scaleFactor;
            panY=getHeight()/2f-(dayY+MONTH_HEIGHT/2f)*scaleFactor;
            clampPan();invalidate();
        }

        private void clampPan(){
            float fw=frozenWidth(),sw=scrollableWidth(),available=getWidth()-fw;
            panX=sw<=available?0:Math.max(available-sw,Math.min(0,panX));
            float h=TABLE_HEIGHT*scaleFactor;
            panY=h<=getHeight()?(getHeight()-h)/2f:Math.max(getHeight()-h,Math.min(0,panY));
        }

        private void zoomAround(float ns,float fx,float fy){
            ns=Math.max(minScale(),Math.min(MAX_SCALE,ns));
            if(Math.abs(ns-scaleFactor)<.0001f)return;
            float cx=(fx-panX)/scaleFactor,cy=(fy-panY)/scaleFactor;
            scaleFactor=ns;panX=fx-cx*ns;panY=fy-cy*ns;clampPan();invalidate();
        }

        @Override protected void onDraw(@NonNull Canvas c){
            super.onDraw(c);c.drawColor(Color.WHITE);
            float fw=frozenWidth(),left=getWidth()-fw;

            c.save();c.clipRect(0,0,left,getHeight());c.translate(panX,panY);c.scale(scaleFactor,scaleFactor);
            for(int i=0;i<MONTHS;i++)drawMonth(c,i);
            c.restore();

            c.save();c.clipRect(left,0,getWidth(),getHeight());
            c.translate(left-DAYS*DAY_WIDTH*scaleFactor,panY);c.scale(scaleFactor,scaleFactor);
            for(int i=0;i<MONTHS;i++)drawMonth(c,i);
            c.restore();
        }

        private void drawMonth(Canvas c,int index){
            Calendar cal=Calendar.getInstance();
            cal.set(selectedYear - 1,START_MONTH,1);cal.add(Calendar.MONTH,index);
            int year=cal.get(Calendar.YEAR),month=cal.get(Calendar.MONTH),days=cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            Calendar first=(Calendar)cal.clone();first.set(year,month,1);
            int empty=first.get(Calendar.DAY_OF_WEEK)-1;
            float top=index*MONTH_HEIGHT,mx=DAYS*DAY_WIDTH,yx=mx+MONTH_WIDTH;

            fill.setColor(Color.WHITE);c.drawRect(0,top,TABLE_WIDTH,top+MONTH_HEIGHT,fill);

            for(int pos=0;pos<DAYS;pos++){
                float x=(DAYS-pos-1)*DAY_WIDTH;
                int day=pos-empty+1,color=Color.WHITE;
                if(day>=1&&day<=days){
                    Calendar d=Calendar.getInstance();d.set(year,month,day);
                    color=d.get(Calendar.DAY_OF_WEEK)==Calendar.SATURDAY?COLOR_BLUE:COLOR_YELLOW;
                }
                fill.setColor(color);c.drawRect(x,top,x+DAY_WIDTH,top+MONTH_HEIGHT,fill);
                if(day>=1&&day<=days){
                    fill.setColor(COLOR_GREEN);
                    c.drawRect(x,top+ROW_HEIGHT*4,x+DAY_WIDTH,top+ROW_HEIGHT*5,fill);
                }
            }

            fill.setColor(COLOR_PINK);c.drawRect(mx,top,mx+MONTH_WIDTH,top+MONTH_HEIGHT,fill);
            fill.setColor(COLOR_YELLOW);
            for(int r:new int[]{0,2,3})c.drawRect(mx,top+ROW_HEIGHT*r,mx+MONTH_WIDTH,top+ROW_HEIGHT*(r+1),fill);
            fill.setColor(COLOR_GREEN);c.drawRect(mx,top+ROW_HEIGHT*4,mx+MONTH_WIDTH,top+ROW_HEIGHT*5,fill);
            fill.setColor(COLOR_BLUE);c.drawRect(mx,top+ROW_HEIGHT*5,mx+MONTH_WIDTH,top+ROW_HEIGHT*7,fill);
            fill.setColor(COLOR_GRAY);c.drawRect(yx,top,yx+YEAR_WIDTH,top+MONTH_HEIGHT,fill);

            centeredText(c,"יום בשבוע",mx,top,MONTH_WIDTH,ROW_HEIGHT,true);
            centeredText(c,englishMonths[month]+"-"+String.valueOf(year).substring(2),mx,top+ROW_HEIGHT,MONTH_WIDTH,ROW_HEIGHT,true);
            centeredText(c,getHebrewMonthName(year,month),mx,top+ROW_HEIGHT*2,MONTH_WIDTH,ROW_HEIGHT,true);
            centeredText(c,"חגים ומועדים",mx,top+ROW_HEIGHT*3,MONTH_WIDTH,ROW_HEIGHT,false);
            centeredText(c,"הוצאות\nחזויות",mx,top+ROW_HEIGHT*5,MONTH_WIDTH,ROW_HEIGHT,true);
            centeredText(c,"הכנסות\nחזויות",mx,top+ROW_HEIGHT*6,MONTH_WIDTH,ROW_HEIGHT,true);
            centeredText(c,String.valueOf(year),yx,top,YEAR_WIDTH,MONTH_HEIGHT,true);

            for(int pos=0;pos<DAYS;pos++){
                float x=(DAYS-pos-1)*DAY_WIDTH;
                centeredText(c,hebrewWeekDays[pos%7],x,top,DAY_WIDTH,ROW_HEIGHT,true);
                int day=pos-empty+1;
                if(day<1||day>days)continue;

                if(isToday(year,month,day)){
                    fill.setColor(COLOR_TODAY_BACKGROUND);
                    c.drawRect(x+dp(2),top+ROW_HEIGHT+dp(2),x+DAY_WIDTH-dp(2),top+ROW_HEIGHT*2-dp(2),fill);
                    todayPaint.setStrokeWidth(dp(3));
                    c.drawRect(x+dp(2),top+ROW_HEIGHT+dp(2),x+DAY_WIDTH-dp(2),top+ROW_HEIGHT*2-dp(2),todayPaint);
                }

                centeredText(c,String.valueOf(day),x,top+ROW_HEIGHT,DAY_WIDTH,ROW_HEIGHT,true);
                centeredText(c,getHebrewDay(year,month,day),x,top+ROW_HEIGHT*2,DAY_WIDTH,ROW_HEIGHT,false);

                String key=String.format(Locale.US,"%04d-%02d-%02d",year,month+1,day);
                List<CalendarEntry> entries=calendarEntries.get(key);
                if(entries!=null&&!entries.isEmpty())drawEntryIndicators(c,entries,x,top);
            }

            drawHolidayRow(c,year,month,days,top,empty);
            drawGrid(c,top);
            drawTodayBorder(c,year,month,top,empty);

            border.setStrokeWidth(dp(3));
            c.drawRect(0,top,TABLE_WIDTH,top+MONTH_HEIGHT,border);
        }

        private boolean isToday(int y,int m,int d){
            Calendar t=Calendar.getInstance();
            return t.get(Calendar.YEAR)==y&&t.get(Calendar.MONTH)==m&&t.get(Calendar.DAY_OF_MONTH)==d;
        }

        private void drawTodayBorder(Canvas c,int year,int month,float top,int empty){
            Calendar t=Calendar.getInstance();
            if(t.get(Calendar.YEAR)!=year||t.get(Calendar.MONTH)!=month)return;
            int pos=empty+t.get(Calendar.DAY_OF_MONTH)-1;
            if(pos<0||pos>=DAYS)return;
            float x=(DAYS-pos-1)*DAY_WIDTH;
            border.setColor(COLOR_TODAY);border.setStrokeWidth(dp(3));
            c.drawRect(x+dp(2),top+dp(2),x+DAY_WIDTH-dp(2),top+MONTH_HEIGHT-dp(2),border);
            border.setColor(Color.BLACK);
        }

        private void drawEntryIndicators(Canvas c,List<CalendarEntry> es,float x,float top){
            boolean event=false,income=false,expense=false,note=false;
            for(CalendarEntry e:es){
                switch(e.type==null?"event":e.type){
                    case"income":income=true;break;
                    case"expense":expense=true;break;
                    case"note":note=true;break;
                    default:event=true;
                }
            }

            List<Integer> colors=new ArrayList<>();
            if(event)colors.add(COLOR_EVENT);
            if(income)colors.add(COLOR_INCOME);
            if(expense)colors.add(COLOR_EXPENSE);
            if(note)colors.add(COLOR_NOTE);

            int n=Math.min(3,colors.size());
            float cx=x+DAY_WIDTH/2f,cy=top+ROW_HEIGHT*5.45f,space=dp(10),start=cx-(n-1)*space/2f;
            fill.setColor(Color.BLACK);

            for(int i=0;i<n;i++){
                fill.setColor(colors.get(i));c.drawCircle(start+i*space,cy,dp(4),fill);
            }

            int hidden=es.size()-n;
            if(hidden>0){
                text.setColor(Color.BLACK);text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setTextSize(dp(9));text.setTextAlign(Paint.Align.LEFT);
                c.drawText("+"+hidden,start+n*space+dp(1),cy+dp(3),text);
            }
        }

        private void drawHolidayRow(Canvas c,int year,int month,int days,float top,int empty){
            Calendar g=Calendar.getInstance();
            for(int day=1;day<=days;day++){
                g.set(year,month,day);
                HebrewCalendar h=new HebrewCalendar();
                h.setTimeInMillis(g.getTimeInMillis());
                int hm=h.get(HebrewCalendar.MONTH),hd=h.get(HebrewCalendar.DAY_OF_MONTH);
                String holiday=null;

                if(hm==0){
                    if(hd<=2)holiday="ראש השנה";
                    else if(hd==3)holiday="צום גדליה";
                    else if(hd==10)holiday="יום כיפור";
                    else if(hd>=15&&hd<=21)holiday="סוכות";
                    else if(hd==22)holiday="שמיני עצרת";
                }else if(hm==2&&hd>=25)holiday="חנוכה";
                else if(hm==3&&hd<=2)holiday="חנוכה";
                else if(hm==4&&hd==15)holiday="ט״ו בשבט";
                else if(hm==5&&hd==14)holiday="פורים";
                else if(hm==6&&hd==14)holiday="פורים";
                else if(hm==7&&hd>=15&&hd<=21)holiday="פסח";
                else if(hm==8&&hd==18)holiday="ל״ג בעומר";
                else if(hm==9&&hd==6)holiday="שבועות";

                if(holiday!=null)drawHoliday(c,day,holiday,top,empty);
            }
        }

        private void drawHoliday(Canvas c,int day,String holiday,float top,int empty){
            int pos=empty+day-1;
            if(pos<0||pos>=DAYS)return;
            float x=(DAYS-pos-1)*DAY_WIDTH;
            fill.setColor(COLOR_PINK);
            c.drawRect(x,top+ROW_HEIGHT*3,x+DAY_WIDTH,top+ROW_HEIGHT*4,fill);
            centeredText(c,holiday,x,top+ROW_HEIGHT*3,DAY_WIDTH,ROW_HEIGHT,false);
        }

        private void drawGrid(Canvas c,float top){
            for(int i=0;i<=DAYS;i++)c.drawLine(i*DAY_WIDTH,top,i*DAY_WIDTH,top+MONTH_HEIGHT,line);
            for(int i=0;i<=7;i++){
                float y=top+i*ROW_HEIGHT;
                c.drawLine(0,y,TABLE_WIDTH,y,line);
            }
        }

        private void centeredText(Canvas c,String value,float x,float y,float w,float h,boolean bold){
            if(value==null||value.isEmpty())return;
            text.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
            text.setColor(Color.BLACK);text.setTextAlign(Paint.Align.CENTER);text.setTextSize(dp(12));
            float cx=x+w/2f;
            if(value.contains("\n")){
                String[] ls=value.split("\n");
                float lh=h/ls.length;
                for(int i=0;i<ls.length;i++){
                    Paint.FontMetrics f=text.getFontMetrics();
                    c.drawText(ls[i],cx,y+lh*i+lh/2f-(f.ascent+f.descent)/2f,text);
                }
            }else{
                Paint.FontMetrics f=text.getFontMetrics();
                c.drawText(value,cx,y+h/2f-(f.ascent+f.descent)/2f,text);
            }
        }

        private String getHebrewMonthName(int year,int month){
            Calendar g=Calendar.getInstance();g.set(year,month,1);
            HebrewCalendar h=new HebrewCalendar();h.setTimeInMillis(g.getTimeInMillis());
            int m=h.get(HebrewCalendar.MONTH);
            boolean leap=h.getActualMaximum(HebrewCalendar.MONTH)==12;
            String[] a=leap&&m>=5
                    ?new String[]{"תשרי","חשון","כסלו","טבת","שבט","אדר א","אדר ב","ניסן","אייר","סיון","תמוז","אב","אלול"}
                    :new String[]{"תשרי","חשון","כסלו","טבת","שבט","אדר","ניסן","אייר","סיון","תמוז","אב","אלול"};
            return m<a.length?a[m]:"";
        }

        private String getHebrewDay(int year,int month,int day){
            Calendar g=Calendar.getInstance();g.set(year,month,day);
            HebrewCalendar h=new HebrewCalendar();h.setTimeInMillis(g.getTimeInMillis());
            return hebrewNumber(h.get(HebrewCalendar.DAY_OF_MONTH));
        }

        private String hebrewNumber(int n){
            if(n<=0)return "";
            String[] u={"","א","ב","ג","ד","ה","ו","ז","ח","ט"};
            if(n<20){
                if(n==15)return"טו";
                if(n==16)return"טז";
                return n>=10?"י"+u[n-10]:u[n];
            }
            if(n<30)return"כ"+u[n-20];
            if(n==30)return"ל";
            if(n==31)return"לא";
            return String.valueOf(n);
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
            @Override public boolean onScaleBegin(ScaleGestureDetector d){
                getParent().requestDisallowInterceptTouchEvent(true);return true;
            }
            @Override public boolean onScale(ScaleGestureDetector d){
                zoomAround(scaleFactor*d.getScaleFactor(),d.getFocusX(),d.getFocusY());return true;
            }
            @Override public void onScaleEnd(ScaleGestureDetector d){
                getParent().requestDisallowInterceptTouchEvent(false);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            scaleDetector.onTouchEvent(e);
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    downX=lastTouchX=e.getX();downY=lastTouchY=e.getY();moved=false;
                    getParent().requestDisallowInterceptTouchEvent(true);return true;

                case MotionEvent.ACTION_POINTER_DOWN:
                    moved=true;return true;

                case MotionEvent.ACTION_MOVE:
                    if(scaleDetector.isInProgress()){moved=true;return true;}
                    float dx=e.getX()-lastTouchX,dy=e.getY()-lastTouchY;
                    if(Math.abs(e.getX()-downX)>TOUCH_SLOP||Math.abs(e.getY()-downY)>TOUCH_SLOP)moved=true;
                    panX+=dx;panY+=dy;clampPan();lastTouchX=e.getX();lastTouchY=e.getY();invalidate();return true;

                case MotionEvent.ACTION_POINTER_UP:
                    if(e.getPointerCount()>1){
                        int i=e.getActionIndex()==0?1:0;
                        if(i<e.getPointerCount()){lastTouchX=e.getX(i);lastTouchY=e.getY(i);}
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if(!moved&&!scaleDetector.isInProgress())handleClick(e.getX(),e.getY());
                    performClick();return true;

                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);return true;
            }
            return true;
        }

        private void handleClick(float sx,float sy){
            float x=(sx-panX)/scaleFactor,y=(sy-panY)/scaleFactor;
            int mi=(int)(y/MONTH_HEIGHT);
            if(mi<0||mi>=MONTHS||x>=DAYS*DAY_WIDTH)return;

            int pos=DAYS-1-(int)(x/DAY_WIDTH);
            if(pos<0||pos>=DAYS)return;

            Calendar selected=Calendar.getInstance();
            selected.set(selectedYear - 1,START_MONTH,1);selected.add(Calendar.MONTH,mi);

            int year=selected.get(Calendar.YEAR),month=selected.get(Calendar.MONTH);
            Calendar first=Calendar.getInstance();first.set(year,month,1);
            int empty=first.get(Calendar.DAY_OF_WEEK)-1,day=pos-empty+1;
            if(day<1||day>selected.getActualMaximum(Calendar.DAY_OF_MONTH))return;

            String gregorian=String.format(Locale.US,"%04d-%02d-%02d",year,month+1,day);
            Calendar date=Calendar.getInstance();date.set(year,month,day);
            HebrewCalendar h=new HebrewCalendar();h.setTimeInMillis(date.getTimeInMillis());

            String hebrew=hebrewNumber(h.get(HebrewCalendar.DAY_OF_MONTH))+" ב"+
                    getHebrewMonthName(year,month)+" "+h.get(HebrewCalendar.YEAR);

            Intent i=new Intent(requireContext(),DayDetailsActivity.class);
            i.putExtra("gregorian_date",gregorian);
            i.putExtra("hebrew_date",hebrew);
            startActivity(i);
        }

        @Override public boolean performClick(){super.performClick();return true;}
    }
}
