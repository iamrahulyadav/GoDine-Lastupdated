package com.netreadystaging.godine.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.netreadystaging.godine.R;
import com.netreadystaging.godine.adapters.SearchRestaurantAdapter;
import com.netreadystaging.godine.callbacks.DrawerLocker;
import com.netreadystaging.godine.controllers.ErrorController;
import com.netreadystaging.godine.controllers.ServiceController;
import com.netreadystaging.godine.models.Restaurant;
import com.netreadystaging.godine.utils.AppGlobal;
import com.netreadystaging.godine.utils.ServiceMod;
import com.netreadystaging.godine.utils.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import in.technobuff.helper.http.HttpResponseCallback;


/**
 * Created by sony on 25-01-2017.
 */

public class SelectnearbyRestaurant extends Fragment  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, AdapterView.OnItemClickListener  {
    View view;
    AppGlobal appGlobal=AppGlobal.getInatance();
    ListView listView;
    TextView mTitle;
    ArrayList<Restaurant> nearbylist = new ArrayList<>();
    SearchRestaurantAdapter adapter;
    private boolean isCurrentLocationSearch;
    GoogleApiClient mgoogleApiclient;
    private double currentLat;
    private double currentLng;
    String amount,id,membersaving;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view=inflater.inflate(R.layout.selectnearbyrestaurant,container,false);
        listView = (ListView)view.findViewById(R.id.listvies);
        adapter = new SearchRestaurantAdapter(getContext(), R.layout.restaurant_search_list, nearbylist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        isCurrentLocationSearch = true;
        if (Utility.checkGooglePlayService(getActivity())) {
            setupLocation();
        }
        ((DrawerLocker)getActivity()).setDrawerLocked(true);
        Bundle bundle  =  getArguments();
        if(bundle!=null) {
            amount = bundle.getString("amount");
            membersaving = bundle.getString("membersaving");
            id = bundle.getString("userId");
        }
        setupToolBar();
        return view;
    }

    protected synchronized void setupLocation() {
        mgoogleApiclient = new GoogleApiClient.Builder(getContext()).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        if (mgoogleApiclient != null) {
            mgoogleApiclient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location mLocation = LocationServices.FusedLocationApi.getLastLocation(mgoogleApiclient);
        if (mLocation != null) {
            currentLat = mLocation.getLatitude();
            currentLng = mLocation.getLongitude();
            Log.d("Current Location", currentLat + "," + currentLng);
            String name = "";
            String zipcode = "";
            String latitude = "";
            String longitude = "";
            String miles = "0";
      if(isCurrentLocationSearch){
                latitude = currentLat+"" ;
                longitude= currentLng+"";
                miles= AppGlobal.getInatance().getMiles();
            }
            loadRestaurants(name, zipcode, latitude, longitude, miles);
        }
    }

    private void loadRestaurants(final String name, final String zipcode, final String latitude, final String longitude, String Miles) {
        Utility.showLoadingPopup(getActivity());
        if (nearbylist != null) {
            nearbylist.clear();
        }

        // For Testing
       // Miles = "100";
        final HashMap<String, String> params = new HashMap<>();
        params.put("RestaurantNameOrCity", name);
        params.put("ZipCode", zipcode);
        params.put("lat", latitude);
        params.put("lng", longitude);
        params.put("miles", Miles);

        new ServiceController(getActivity(), new HttpResponseCallback() {
            @Override
            public void response(boolean success, boolean fail, String data) {
                Utility.hideLoadingPopup();
                if (success) {
                    JSONArray jsonArray = null;
                    try {
                        String Result="";
                        String Message="";
                        jsonArray = new JSONArray(data);
                        if (jsonArray.length() > 0) {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObjects = jsonArray.getJSONObject(i);
                                Result=jsonObjects.getString("Result");
                                Message=jsonObjects.getString("Message");
                                if(Result.equalsIgnoreCase("Success")) {
                                    Restaurant restaurantObj = new Restaurant();
                                    // String Id=jsonObjects.getString("Id");
                                    restaurantObj.setId(jsonObjects.getString("Id"));
                                    restaurantObj.setImage(jsonObjects.getString("RestaurantImage"));
                                    restaurantObj.setName(jsonObjects.getString("Name"));
                                    restaurantObj.setReview(jsonObjects.getString("NumberOfReviews"));
                                    restaurantObj.setAddress(jsonObjects.getString("Address"));
                                    StringBuffer Area = new StringBuffer();
                                    Area.append(jsonObjects.getString("Region"));
                                    Area.append("," + jsonObjects.getString("City"));
                                    Area.append("," + jsonObjects.getString("PostalCode"));
                                    restaurantObj.setArea("" + Area);
                                    restaurantObj.setRestaurantCusine(jsonObjects.getString("RestaurantCuisine"));
                                    restaurantObj.setLunch(jsonObjects.getString("RestaurantAverageLunch"));
                                    restaurantObj.setDinner(jsonObjects.getString("RestaurantAverageDinner"));
                                    restaurantObj.setRating((float) jsonObjects.getDouble("Rating"));
                                    double lat = jsonObjects.getDouble("Latitude");
                                    double lng = jsonObjects.getDouble("Longitude");
                                    long miles = calculateMiles(lat, lng);
                                    restaurantObj.setMiles(miles);
                                    int availableOffers = jsonObjects.getInt("IsOfferAvailable");
                                    restaurantObj.setOffers(availableOffers);
                                    nearbylist.add(restaurantObj);
                                }
                                    else
                                {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                    builder.setTitle("Info");
                                    builder.setCancelable(false);
                                    builder.setMessage(Message);
                                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            MemberVerification frag=new MemberVerification();
                                            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.flContent,frag).commit();
                                        }
                                    });
                                    builder.create();
                                    builder.show();
                                    }
                                adapter.notifyDataSetChanged();
                            }
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("Info");
                            builder.setCancelable(false);
                            builder.setMessage("No Restaurant Found at your location,Please check again");
                            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    MemberVerification frag=new MemberVerification();
                                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.flContent,frag).commit();
                                }
                            });
                            builder.create();
                            builder.show();
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    ErrorController.showError(getContext(), data, success);
                }
            }
        }).request(ServiceMod.SEARCH_RESTAURANT, params);
    }

    private long calculateMiles(double lat, double lng) {
        Location locationA = new Location("point A");

        locationA.setLatitude(currentLat);
        locationA.setLongitude(currentLng);

        Location locationB = new Location("point B");

        locationB.setLatitude(lat);
        locationB.setLongitude(lng);

        float distance = locationA.distanceTo(locationB);
        long miles = (long) (distance * 0.000621371);
        return miles;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }



    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String restid = ((TextView) view.findViewById(R.id.idd)).getText().toString();
       HashMap<String, String> params = new HashMap<>();
        params.put("UserId", id);
        params.put("RestaurantId", restid);
       params.put("CheckAmount", amount);
       params.put("Savings", membersaving);
       Utility.showLoadingPopup(getActivity());
        new ServiceController(getContext(), new HttpResponseCallback() {
            @Override
            public void response(boolean success, boolean fail, String data) {
                if (success) {
                    Utility.hideLoadingPopup();
                    JSONArray jsonArray=null;
                    try {
                        jsonArray=new JSONArray(data);
                        for (int i=0;i<jsonArray.length();i++)
                        {
                            JSONObject jsonObject=null;
                            jsonObject=jsonArray.getJSONObject(i);
                            String Result=jsonObject.getString("Result");
                            if(Result.equalsIgnoreCase("Success"))
                            {

                                Log.d("Muhib", data);
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setTitle("Info");
                                builder.setCancelable(false);
                                builder.setMessage("Data saved successfully");
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        ProfilePageFragment frag=new ProfilePageFragment();
                                       getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.flContent,frag).commit();

                                    }
                                });
                                builder.create();
                                builder.show();
                            }
                            else if(Result.equalsIgnoreCase("Error"))
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setTitle("Info");
                                builder.setCancelable(false);
                                builder.setMessage("Savings amount cannot be greater than check amount");
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();

                                    }
                                });
                                builder.create();
                                builder.show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    ErrorController.showError(getContext(), data, success);
                }
            }
        }).request(ServiceMod.InsertRestaurantSavings, params);

    }
    private void setupToolBar() {
        Activity activity = getActivity();
        Toolbar toolBar  =  (Toolbar) activity.findViewById(R.id.toolbar) ;
        toolBar.setVisibility(View.VISIBLE);
        mTitle = (TextView) toolBar.findViewById(R.id.tvToolBarMiddleLabel);

    }

    @Override
    public void onResume() {
        super.onResume();
        mTitle.setText("Select Restaurant");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((DrawerLocker)getActivity()).setDrawerLocked(false);

    }
}

