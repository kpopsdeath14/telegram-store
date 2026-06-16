(ns {{name}}.pages.information.information
  (:require
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as str]
   [{{name}}.pages.catalog.logo :refer [logo]]))


(defn is-mobile? []
  (let [user-agent (.-userAgent (.-navigator js/window))
        mobile-regex #"(?i)android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini"]
    (boolean (re-find mobile-regex user-agent))))


(defn policy-label [policy]
  (let [name (some-> (:policy_name policy) str/lower-case)]
    (cond
      (and name (re-find #"offer|оферт" name)) "Договор оферты"
      (and name (re-find #"cookies" name)) "Согласие Cookies"
      (and name (re-find #"personal_data_agreement" name)) "Согласие на обработку ПДн"
      (and name (re-find #"personal_data" name)) "Политика обработки персональных данных"
      (seq (:policy_title policy)) (:policy_title policy)
      :else "Документ")))

(defn information_page []
  (let [policies (reagent/cursor app-state [:policies_all])
        web-app (.-WebApp js/Telegram)
        platform (when web-app (.-platform web-app))
        is-tg-mobile? (contains? #{"ios" "android"} platform)
        is-mobile-device? (is-mobile?)
        is-mobile (or is-tg-mobile? is-mobile-device?)
        ]
    (fn []
      [:div {:style {:background "#ffffff"}}
       [logo]
       [:div {:style {:paddingTop 24
                      :paddingRight 20
                      :paddingBottom 36
                      :paddingLeft 20
                      }
              }
        [:div {:style {:fontSize 32
                       :fontWeight 400
                       :color "#111"
                       :marginBottom 16}}
         "Информация"]
        [:div {:style {:display "flex"
                       :flexDirection "column"
                       :gap 12}}
         (for [policy @policies]
           (let [label (policy-label policy)]
             ^{:key (or (:policy_name policy) label)}
             [:button {:type "button" 
                       :onClick (fn []
                                  (.openLink web-app (:policy_title policy))
                                  )
                       :style {:display "flex"
                               :alignItems "center"
                               :justifyContent "space-between"
                               :width "100%"
                               :padding "10px 0"
                               :border "none"
                               :background "transparent"
                               :textAlign "left"
                               }
                       }
              [:span {:style {:fontSize 18
                              :fontWeight 400
                              :color "#111"}}
               label]
              [:> icons/CaretRightOutlined {:style {:fontSize 18
                                                    :color "var(--color-accent)"}
                                            }
               ]
              ]
              )
              ) 
              ]
              ]
              ]
              )
              )
              )
