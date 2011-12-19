/*
 Copyright 2011 Google, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.adwhirl.adapters;

import android.app.Activity;
import android.util.Log;
import com.adwhirl.*;
import com.adwhirl.AdWhirlLayout.ViewAdRunnable;
import com.adwhirl.obj.Ration;
import com.adwhirl.util.AdWhirlUtil;
import com.google.ads.*;
import com.google.ads.AdRequest.ErrorCode;
import java.text.SimpleDateFormat;

public class GoogleAdMobAdsAdapter extends AdWhirlAdapter implements AdListener {
  public GoogleAdMobAdsAdapter(AdWhirlLayout adWhirlLayout, Ration ration) {
    super(adWhirlLayout, ration);
  }

  protected String birthdayForAdWhirlTargeting() {
    return (AdWhirlTargeting.getBirthDate() != null) ?
      new SimpleDateFormat("yyyyMMdd").
          format(AdWhirlTargeting.getBirthDate().getTime()) : null;
  }

  protected AdRequest.Gender genderForAdWhirlTargeting() {
    switch (AdWhirlTargeting.getGender()) {
      case MALE:   return AdRequest.Gender.MALE;
      case FEMALE: return AdRequest.Gender.FEMALE;
      default:     return null;
    }
  }

  @Override
  public void handle() {
    AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();

    if (adWhirlLayout == null) {
      return;
    }
    
    Activity activity = adWhirlLayout.activityReference.get();
    if (activity == null) {
      return;
    }

    AdView adView = new AdView(activity, AdSize.BANNER, ration.key);

    adView.setAdListener(this);
    adView.loadAd(requestForAdWhirlLayout(adWhirlLayout));
  }
  
  protected void log(String message) {
    Log.d(AdWhirlUtil.ADWHIRL, "GoogleAdapter " + message);
  }
  
  protected AdRequest requestForAdWhirlLayout(AdWhirlLayout layout) {
    AdRequest result = new AdRequest();

    result.setTesting(AdWhirlTargeting.getTestMode());
    result.setGender(genderForAdWhirlTargeting());
    result.setBirthday(birthdayForAdWhirlTargeting());

    if (layout.extra.locationOn == 1) {
      result.setLocation(layout.adWhirlManager.location);
    }

    result.setKeywords(AdWhirlTargeting.getKeywordSet());
    
    return result;
  }
  
  @Override
  public void onDismissScreen(Ad arg0) {
  }

  @Override
  public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {
    log("failure (" + arg1 + ")");

    arg0.setAdListener(null);

    AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();

    if (adWhirlLayout == null) {
      return;
    }

    adWhirlLayout.rollover();
  }

  @Override
  public void onLeaveApplication(Ad arg0) {
  }

  @Override
  public void onPresentScreen(Ad arg0) {
  }

  @Override
  public void onReceiveAd(Ad arg0) {
    log("success");

    AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();

    if (adWhirlLayout == null) {
      return;
    }
    
    if (!(arg0 instanceof AdView)) {
      log("invalid AdView");
      return;
    }

    AdView adView = (AdView)arg0;
    
    adWhirlLayout.adWhirlManager.resetRollover();
    adWhirlLayout.handler.post(new ViewAdRunnable(adWhirlLayout, adView));
    adWhirlLayout.rotateThreadedDelayed();
  } 
}
