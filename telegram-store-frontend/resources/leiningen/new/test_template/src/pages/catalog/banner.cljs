(ns {{name}}.pages.catalog.banner
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent]
   [{{name}}.apiurimaker :refer [banner_uri_maker]]))

(defn banner []
  (let [Carousel antd/Carousel
        banners (reagent/cursor app-state [:banners])]
    (fn []
      (let [banner (first (filter (fn [item] (= "main_page" (:banner_location item))) @banners))
            images (vec (take 4 (:banner_images banner)))
            banner-height 300]
        (when (seq images)
          [:div {:style {:width "100%"
                         :height banner-height
                         :overflow "hidden"}}
           [:> Carousel {:dots false
                         :autoplay true
                         :autoplaySpeed 2700
                         :speed 300
                         :effect "fade"}
            (for [[idx file] (map-indexed vector images)]
              ^{:key (str "banner-" idx "-" file)}
              [:div {:style {:width "100%"
                             :height banner-height}}
               [:img {:src (banner_uri_maker file)
                      :alt ""
                      :style {:width "100%"
                              :height "100%"
                              :objectFit "cover"
                              :objectPosition "center"
                              :display "block"}}]])]])))))
