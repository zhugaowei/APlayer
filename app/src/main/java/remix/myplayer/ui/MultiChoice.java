package remix.myplayer.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import remix.myplayer.R;
import remix.myplayer.adapter.AlbumAdapter;
import remix.myplayer.adapter.ArtistAdapter;
import remix.myplayer.adapter.FolderAdapter;
import remix.myplayer.adapter.PlayListAdapter;
import remix.myplayer.adapter.SongAdapter;
import remix.myplayer.bean.mp3.PlayList;
import remix.myplayer.interfaces.OnMultiItemClickListener;
import remix.myplayer.interfaces.OnUpdateOptionMenuListener;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.fragment.AlbumFragment;
import remix.myplayer.ui.fragment.ArtistFragment;
import remix.myplayer.ui.fragment.FolderFragment;
import remix.myplayer.ui.fragment.PlayListFragment;
import remix.myplayer.ui.fragment.SongFragment;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

/**
 * @ClassName MultiChoice
 * @Description 多选操作类
 * @Author Xiaoborui
 * @Date 2016/9/20 16:12
 */
public class MultiChoice implements OnMultiItemClickListener {
    private Context mContext;

    /** 当前正在操作的activity或者fragment */
    public static String TAG = "";

    /** 多选的操作类型，包括专辑、艺术家、播放列表、普通歌曲、播放列表下的歌曲 */
    public static int TYPE = -1;

    /** 多选菜单是否正在显示 */
    private boolean mIsShow = false;

    private RecyclerView.Adapter mAdapter;

//    /** 所有选中状态的view */
//    public ArrayList<View> mSelectedViews = new ArrayList<>();

    /** 所有选中view的position */
    private Set<Integer> mSelectedPosition = new HashSet<>();

    /** 所有选中view对应的参数 包括歌曲id 专辑id 艺术家id 文件夹名 播放列表名 */
    private ArrayList<Object> mSelectedArg = new ArrayList<>();

    /** 更新optionmenu */
    private OnUpdateOptionMenuListener mUpdateOptionMenuListener;

    public MultiChoice(Context context){
        mContext = context;
    }

    public MultiChoice(){}

    public boolean isShow(){
        return mIsShow;
    }

    public void setShowing(boolean showing){
        mIsShow = showing;
    }

    public void setAdapter(RecyclerView.Adapter adapter){
        mAdapter = adapter;
    }

    public Set<Integer> getSelectPos(){
        return mSelectedPosition;
    }

    @Override
    public void OnAddToPlayQueue() {
        int num;
        ArrayList<Integer> idList = new ArrayList<>();
        switch (TYPE){
            case Constants.SONG:
            case Constants.PLAYLISTSONG:
                for(Object arg : mSelectedArg){
                    if (arg instanceof Integer)
                        idList.add((Integer) arg);
                }
                break;
            case Constants.ALBUM:
            case Constants.ARTIST:
            case Constants.FOLDER:
            case Constants.PLAYLIST:
                for(Object arg : mSelectedArg){
                    List<Integer> tempList = MediaStoreUtil.getSongIdList(arg,TYPE);
                    if(tempList != null && tempList.size() > 0)
                        idList.addAll(MediaStoreUtil.getSongIdList(arg,TYPE));
                }
                break;
        }

        num = Global.AddSongToPlayQueue(idList);
        ToastUtil.show(mContext,mContext.getString(R.string.add_song_playqueue_success,num));
        updateOptionMenu(false);
    }

    @Override
    public void OnAddToPlayList() {
        final ArrayList<Integer> idList = new ArrayList<>();
        switch (TYPE){
            case Constants.SONG:
            case Constants.PLAYLISTSONG:
                for(Object arg : mSelectedArg){
                    if (arg instanceof Integer)
                        idList.add((Integer) arg);
                }
                break;
            case Constants.ALBUM:
            case Constants.ARTIST:
            case Constants.FOLDER:
            case Constants.PLAYLIST:
                for(Object arg : mSelectedArg){
                    List<Integer> tempList = MediaStoreUtil.getSongIdList(arg,TYPE);
                    if(tempList != null && tempList.size() > 0)
                        idList.addAll(MediaStoreUtil.getSongIdList(arg,TYPE));
                }
                break;
        }

        //获得所有播放列表的信息
        final List<PlayList> playListInfoList = PlayListUtil.getAllPlayListInfo();
        final ArrayList<String> playlistNameList = new ArrayList<>();
        if(playListInfoList == null)
            return;
        for(int i = 0 ; i < playListInfoList.size();i++){
            playlistNameList.add(playListInfoList.get(i).Name);
        }
        new MaterialDialog.Builder(mContext)
                .title(R.string.add_to_playlist)
                .titleColorAttr(R.attr.text_color_primary)
                .buttonRippleColorAttr(R.attr.ripple_color)
                .theme(ThemeStore.getMDDialogTheme())
                .items(playlistNameList)
                .itemsColorAttr(R.attr.text_color_primary)
                .itemsCallback((dialog, view, which, text) -> {
                    final int num = PlayListUtil.addMultiSongs(idList,playListInfoList.get(which).Name,playListInfoList.get(which)._Id);
                    ToastUtil.show(mContext,mContext.getString(R.string.add_song_playlist_success,num,playListInfoList.get(which).Name));
                    updateOptionMenu(false);
                })
                .neutralText(R.string.create_playlist)
                .neutralColorAttr(R.attr.text_color_primary)
                .onNeutral((dialog, which) -> new MaterialDialog.Builder(mContext)
                        .title(R.string.new_playlist)
                        .titleColorAttr(R.attr.text_color_primary)
                        .buttonRippleColorAttr(R.attr.ripple_color)
                        .positiveText(R.string.create)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeText(R.string.cancel)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .backgroundColorAttr(R.attr.background_color_3)
                        .content(R.string.input_playlist_name)
                        .contentColorAttr(R.attr.text_color_primary)
                        .inputRange(1,15)
                        .input("", mContext.getString(R.string.local_list) + Global.PlayList.size(), (dialog1, input) -> {
                            if(!TextUtils.isEmpty(input)){
                                final int num;
                                int newPlayListId = PlayListUtil.addPlayList(input.toString());
                                ToastUtil.show(mContext, newPlayListId > 0 ?
                                                R.string.add_playlist_success :
                                                newPlayListId == -1 ? R.string.add_playlist_error : R.string.playlist_already_exist,
                                        Toast.LENGTH_SHORT);
                                if(newPlayListId < 0){
                                    return;
                                }
                                num = PlayListUtil.addMultiSongs(idList,input.toString(),newPlayListId);
                                ToastUtil.show(mContext,mContext.getString(R.string.add_song_playlist_success,num,input.toString()));
                                updateOptionMenu(false);
                            }
                        })
                        .show())
                .backgroundColorAttr(R.attr.background_color_3).build().show();
    }

    @Override
    public void OnDelete(boolean deleteSource) {
        int num = 0;
        ArrayList<Integer> idList = new ArrayList<>();
        switch (TYPE){
            case Constants.PLAYLIST:
                for(Object arg : mSelectedArg){
                    if (arg instanceof Integer) {
                        if((Integer)arg == Global.MyLoveID)
                            continue;
                        idList.add((Integer) arg);
                        //保存删除前，选中的播放列表下一共有多少歌曲
                        List<Integer> selectIDList = PlayListUtil.getIDList((Integer) arg);
                        if(selectIDList != null)
                            num += selectIDList.size();
                    }
                }
                PlayListUtil.deleteMultiPlayList(idList);
                break;
            case Constants.PLAYLISTSONG:
                for(Object arg : mSelectedArg){
                    if (arg instanceof Integer)
                        idList.add((Integer) arg);
                }
                num = PlayListUtil.deleteMultiSongs(idList,ChildHolderActivity.ID);
                break;
            case Constants.SONG:
            case Constants.ALBUM:
            case Constants.ARTIST:
            case Constants.FOLDER:
                for(Object arg : mSelectedArg){
                    num += MediaStoreUtil.delete((Integer) arg,TYPE,deleteSource);
                }
                break;
        }
        ToastUtil.show(mContext,mContext.getString(R.string.delete_multi_song,num));
//        if(num > 0){
//            mContext.sendBroadcast(new Intent(MusicService.ACTION_MEDIA_CHANGE));
//        }
        updateOptionMenu(false);
    }


    public void setOnUpdateOptionMenuListener(OnUpdateOptionMenuListener l){
        mUpdateOptionMenuListener = l;
    }

    /**
     * @param adapter
     * @param position
     * @param arg
     * @param tag
     * @return
     */
    public boolean itemClick(RecyclerView.Adapter adapter, int position, Object arg, String tag){
        if(mIsShow && TAG.equals(tag)){
            mIsShow = true;
//            removeOrAddView(view);
            removeOrAddPosition(position);
            removeOrAddArg(arg);
            if(isLibraryAdapter())
                position += 1;
            mAdapter.notifyItemChanged(position);
            return true;
        }
        return false;
    }

    /**
     *
     * @param adapter
     * @param position
     * @param arg
     * @param newTag
     */
    public void itemLongClick(RecyclerView.Adapter adapter, int position, Object arg, String newTag, int type){
        //当前没有处于多选状态
        if(!mIsShow && TAG.equals("")){
            Util.vibrate(mContext,150);
            TAG = newTag;
            TYPE = type;
            mIsShow = true;
            if(mUpdateOptionMenuListener != null)
                mUpdateOptionMenuListener.onUpdate(true);
        }
//        removeOrAddView(view);
        removeOrAddPosition(position);
        removeOrAddArg(arg);
        if(isLibraryAdapter())
            position += 1;
        mAdapter.notifyItemChanged(position);
    }

    private boolean isLibraryAdapter(){
        return mAdapter instanceof SongAdapter || mAdapter instanceof AlbumAdapter || mAdapter instanceof ArtistAdapter
                || mAdapter instanceof PlayListAdapter || mAdapter instanceof FolderAdapter;
    }

    public void updateOptionMenu(boolean multishow){
        if(mUpdateOptionMenuListener != null)
            mUpdateOptionMenuListener.onUpdate(multishow);
        mAdapter.notifyDataSetChanged();
    }

//    public void addView(View view){
//        mSelectedViews.add(view);
//        setViewSelected(view, true);
//    }

//    /**
//     * 添加或者删除选中的view
//     * @param view
//     */
//    public void removeOrAddView(View view){
//        if(mSelectedViews.contains(view)){
//            mSelectedViews.remove(view);
//
//            setViewSelected(view,false);
//        } else {
//            mSelectedViews.add(view);
//            setViewSelected(view,true);
//        }
//    }

    /**
     * 添加或者删除选中view在adapter中的position
     * @param position
     */
    public void removeOrAddPosition(int position){
        if(mSelectedPosition.contains(position))
            mSelectedPosition.remove(Integer.valueOf(position));
        else {
            mSelectedPosition.add(position);
        }
    }

    /**
     * 添加或者删除选中的view对应的参数，如歌曲id
     * @param arg
     */
    public void removeOrAddArg(Object arg){
        if(mSelectedArg.contains(arg)){
            mSelectedArg.remove(arg);
        } else {
            mSelectedArg.add(arg);
        }
    }

    /**
     * 重置
     */
    public void clear(){
//        clearSelectedViews();
//        mSelectedViews.clear();
        mSelectedPosition.clear();
        mSelectedArg.clear();
        TAG = "";
        TYPE = -1;
    }

//    /**
//     * 清除所有view的选中状态
//     */
//    public void clearSelectedViews(){
//        for(View view : mSelectedViews){
//            if(view != null)
//                setViewSelected(view,false);
//        }
//        mSelectedViews.clear();
//    }

    /**
     * 设置view的选中状态
     * @param v
     * @param selected
     */
    public void setViewSelected(View v,boolean selected){
        if(v != null) {
            v.setSelected(selected);
        }
    }

    public static int getType(String tag){
        if(tag.equals(SongFragment.TAG))
            return Constants.SONG;
        else if(tag.equals(AlbumFragment.TAG))
            return Constants.ALBUM;
        else if (tag.equals(ArtistFragment.TAG))
            return Constants.ARTIST;
        else if(tag.equals(FolderFragment.TAG))
            return Constants.FOLDER;
        else if(tag.equals(PlayListFragment.TAG))
            return Constants.PLAYLIST;
        else if(tag.equals(ChildHolderActivity.TAG))
            return Constants.SONG;
        else if(tag.equals(ChildHolderActivity.TAG_PLAYLIST_SONG))
            return Constants.PLAYLISTSONG;
        else
            return -1;
    }
}
