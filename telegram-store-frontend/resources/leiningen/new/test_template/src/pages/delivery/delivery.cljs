(ns {{name}}.pages.delivery.delivery
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   [{{name}}.events.cdek-search-city :refer [cdek_search_city]]
   [{{name}}.events.cdek-search-pvz :refer [cdek_search_pvz]]
   [{{name}}.pages.delivery.methods.cdek :as cdek-method]
   [{{name}}.pages.delivery.methods.self-taken :as self-taken-method]
   [reagent.core :as reagent]
   [clojure.string :as str]
   )
  )



(defn is-mobile? []
  (let [user-agent (.-userAgent (.-navigator js/window))
        mobile-regex #"(?i)android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini"]
    (boolean (re-find mobile-regex user-agent))))

(defn parse-delivery-price [value]
  (cond
    (number? value) value
    (string? value) (let [normalized (str/replace value #"," ".")
                          parsed (js/parseFloat normalized)]
                      (if (js/isNaN parsed) 0 parsed))
    :else 0))

(defn method-connection-attr [method key]
  (let [attrs (:connection_attributes method)
        string-key (name key)]
    (or (get attrs key)
        (get attrs string-key)
        "")))

(defn method-default-price [method]
  (parse-delivery-price (method-connection-attr method :default_price)))

(defn method-address [method]
  (method-connection-attr method :address))

(defn viewport-height []
  (let [vv (.-visualViewport js/window)]
    (or (when vv (.-height vv))
        (.-innerHeight js/window))))

(defonce body-lock-state (reagent/atom {:locked? false :scrollY 0}))

(defn set-body-lock! [locked?]
  (let [body (.-body js/document)
        html (.-documentElement js/document)
        style (.-style body)
        html-style (.-style html)]
    (if locked?
      (when-not (:locked? @body-lock-state)
        (let [scroll-y (.-scrollY js/window)]
          (swap! body-lock-state assoc :locked? true :scrollY scroll-y)
          (set! (.-position style) "fixed")
          (set! (.-top style) (str (- scroll-y) "px"))
          (set! (.-width style) "100%")
          (set! (.-overflow style) "hidden")
          (set! (.-height html-style) "100%")
          (set! (.-overflow html-style) "hidden")
          (set! (.-touchAction html-style) "none")))
      (when (:locked? @body-lock-state)
        (let [scroll-y (:scrollY @body-lock-state)]
          (swap! body-lock-state assoc :locked? false)
          (set! (.-position style) "")
          (set! (.-top style) "")
          (set! (.-width style) "")
          (set! (.-overflow style) "")
          (set! (.-height html-style) "")
          (set! (.-overflow html-style) "")
          (set! (.-touchAction html-style) "")
          (.scrollTo js/window 0 scroll-y))))))

(defn parse-int-safe [value]
  (try
    (cond
      (number? value) (int value)
      (string? value) (let [parsed (js/parseInt value 10)]
                        (when-not (js/isNaN parsed) parsed))
      :else nil)
    (catch :default _ nil)))

(defn method-tariff-code [method]
  (let [single (method-connection-attr method :tariff_code)
        multi (method-connection-attr method :tariff_codes)]
    (cond
      (some? (parse-int-safe single)) (parse-int-safe single)
      (sequential? multi) (some parse-int-safe multi)
      (string? multi) (->> (str/split multi #"[ ,]+")
                           (map parse-int-safe)
                           (remove nil?)
                           first)
      :else nil)))

(def cdek-tariff-defaults
  {136 {:label "Склад — склад" :from :pvz :to :pvz}
   137 {:label "Склад — дверь" :from :pvz :to :door}
   138 {:label "Дверь — склад" :from :door :to :pvz}
   139 {:label "Дверь — дверь" :from :door :to :door}})

(def cdek-tariff-order [136 137 138 139])

(defn parse-bool [value]
  (cond
    (boolean? value) value
    (string? value) (contains? #{"true" "1" "yes" "y"} (str/lower-case value))
    :else false))

(defn cdek-tariff-raw [tariffs code]
  (let [code-str (str code)
        code-kw (keyword code-str)]
    (or (get tariffs code)
        (get tariffs code-str)
        (get tariffs code-kw)
        {})))

(defn method-cdek-tariffs [method]
  (let [attrs (:connection_attributes method)
        tariffs (or (get attrs :tariffs) (get attrs "tariffs") {})
        fallback-from-address (or (get attrs :from_address) (get attrs "from_address"))
        fallback-from-pvz-code (or (get attrs :from_pvz_code) (get attrs "from_pvz_code"))
        fallback-from-city (or (get attrs :from_city) (get attrs "from_city"))
        fallback-from-city-code (or (get attrs :from_city_code) (get attrs "from_city_code"))]
    (->> cdek-tariff-order
         (keep (fn [code]
                 (let [cfg (cdek-tariff-raw tariffs code)
                       enabled? (if (or (contains? cfg :enabled) (contains? cfg "enabled"))
                                  (parse-bool (or (:enabled cfg) (get cfg "enabled")))
                                  true)
                       default (get cdek-tariff-defaults code {})
                       label (or (:label cfg)
                                 (get cfg "label")
                                 (:name cfg)
                                 (get cfg "name")
                                 (:label default)
                                 (str code))
                       raw-from-address (or (:from_address cfg)
                                            (get cfg "from_address"))
                       raw-from-pvz (or (:from_pvz_code cfg)
                                        (get cfg "from_pvz_code"))
                       from-address (if (str/blank? (or raw-from-address ""))
                                      fallback-from-address
                                      raw-from-address)
                       from-pvz-code (if (str/blank? (or raw-from-pvz ""))
                                       fallback-from-pvz-code
                                       raw-from-pvz)]
                   (when enabled?
                     (cond-> {:code code
                              :label label
                              :from (:from default)
                              :to (:to default)}
                       (some? from-address) (assoc :from_address from-address)
                       (some? from-pvz-code) (assoc :from_pvz_code from-pvz-code)
                       (some? fallback-from-city) (assoc :from_city fallback-from-city)
                       (some? fallback-from-city-code) (assoc :from_city_code fallback-from-city-code))))))
         vec)))

(defn cdek-tariffs-configured? [method]
  (let [attrs (:connection_attributes method)]
    (or (and (map? attrs) (contains? attrs :tariffs))
        (and (map? attrs) (contains? attrs "tariffs")))))

(defn cdek-tariff-options [method]
  (let [opts (method-cdek-tariffs method)
        configured? (cdek-tariffs-configured? method)]
    (cond
      (seq opts) opts
      configured? []
      :else (mapv (fn [code]
                    (merge {:code code}
                           (get cdek-tariff-defaults code)))
                  cdek-tariff-order))))

(defn phone_input [input-style input-class input-id delivery-errors]
  (let [value (reagent/atom "+7")
        phone (reagent/cursor app-state [:shipping_data :phone])]

    (fn []
      (let [handle-change (fn [e]
                            (let [new-value (.. e -target -value)
                                  raw-digits (str/replace new-value #"\D" "")
                                  digits (if (str/starts-with? raw-digits "7")
                                           (subs raw-digits 1)
                                           raw-digits)
                                  formatted (cond
                                              (empty? digits) ""
                                              (<= (count digits) 10)
                                              (str "+7 ("
                                                   (when (seq digits) (subs digits 0 3))
                                                   (when (> (count digits) 3) (str ") " (subs digits 3 6)))
                                                   (when (> (count digits) 6) (str "-" (subs digits 6 8)))
                                                   (when (> (count digits) 8) (str "-" (subs digits 8 10))))
                                              :else @value)
                                  phone-value (when (seq digits) (str "+7" digits))]
                              (reset! value formatted)
                              (reset! phone phone-value)
                              (when (and (contains? @delivery-errors :phone)
                                         (re-matches #"^\+7\d{10}$" (or phone-value "")))
                                (swap! app-state update :delivery_errors dissoc :phone))))]

        [:> antd/Input
         {:value @value
          :on-change handle-change
          :inputMode "tel"
          :placeholder "Телефон"
          :maxLength 18
          :style input-style
          :className input-class
          :id input-id}]))))


(defn delivery_page []
  (let [web-app (.-WebApp js/Telegram)
        Input antd/Input
        Radio antd/Radio
        RadioGroup (.-Group Radio)
        delivery_provider_name (reagent/cursor app-state [:shipping_data :delivery_provider_name])
        payment_type (reagent/cursor app-state [:shipping_data :payment_type])
        payment_methods (reagent/cursor app-state [:payment_methods])
        delivery_methods (reagent/cursor app-state [:delivery_methods])
        delivery-errors (reagent/cursor app-state [:delivery_errors])
        cdek-city-suggestions (reagent/cursor app-state [:cdek_city_suggestions])
        cdek-city-loading? (reagent/cursor app-state [:cdek_city_loading?])
        cdek-city-error (reagent/cursor app-state [:cdek_city_error])
        cdek-delivery-points (reagent/cursor app-state [:cdek_delivery_points])
        cdek-delivery-points-loading? (reagent/cursor app-state [:cdek_delivery_points_loading?])
        cdek-delivery-points-error (reagent/cursor app-state [:cdek_delivery_points_error])
        city-drawer-open? (reagent/atom false)
        pvz-drawer-open? (reagent/atom false)
        city-input-ref (reagent/atom nil)
        pvz-input-ref (reagent/atom nil)
        drawer-height (reagent/atom nil)
        city-query (reagent/atom "")
        pvz-query (reagent/atom "")
        delivery_cost (reagent/cursor app-state [:shipping_data :delivery_cost])
        cart_summary (reagent/cursor app-state [:cart_summary])
        page-style {:minHeight "100vh"
                    :paddingRight 20
                    :paddingBottom 36
                    :paddingLeft 20
                    :background "#ffffff"
                    :color "#111"}
        container-style {:maxWidth 520
                         :margin "0 auto"
                         :display "flex"
                         :flexDirection "column"
                         :gap 26}
        section-style {:display "flex"
                       :flexDirection "column"
                       :gap 10}
        section-title-style {:fontSize 32
                             :fontWeight 400
                             :color "#111"
                             :marginBottom 4}
        input-style {:border "1px solid var(--color-border)"
                     :borderRadius 6
                     :width "100%"
                     :height 46
                     :padding "0 16px"
                     :fontSize 18
                     :color "#111"
                     :background "#fff"}
        hint-text-style {:fontSize 12
                         :color "#8c8c8c"}
        radio-group-style {:display "flex"
                           :flexDirection "column"
                           :gap 10
                           :marginTop 2
                           :marginBottom 6}
        summary-style {:display "flex"
                       :flexDirection "column"
                       :alignItems "flex-end"
                       :gap 6
                       :marginTop 8
                       :width "100%"}
        summary-line-style {:fontSize 18
                            :fontWeight 400
                            :textAlign "right"
                            :color "#676767"}
        total-style {:marginTop 12
                     :fontSize 24
                     :fontWeight 600
                     :color "#111"
                     :textAlign "right"}
        drawer-wrap-style {:position "fixed"
                           :left 0
                           :right 0
                           :top 0
                           :height (let [h @drawer-height]
                                     (if (number? h) (str h "px") "100vh"))
                           :touchAction "none"
                           :zIndex 1000}
        drawer-mask-style {:position "absolute"
                           :inset 0
                           :background "rgba(0,0,0,0.4)"}
        drawer-sheet-style {:position "absolute"
                            :left 0
                            :right 0
                            :bottom 0
                            :height "85%"
                            :background "#ffffff"
                            :borderRadius "16px 16px 0 0"
                            :display "flex"
                            :flexDirection "column"
                            :boxShadow "0 -8px 24px rgba(0,0,0,0.08)"}
        drawer-body-style {:display "flex"
                           :flexDirection "column"
                           :gap 12
                           :padding "16px 16px 24px"
                           :flex "1 1 auto"
                           :minHeight 0
                           :overflowY "auto"
                           :WebkitOverflowScrolling "touch"
                           :overscrollBehavior "contain"
                           :touchAction "pan-y"
                           :background "#ffffff"
                           :color "#111111"}
        drawer-input-style (merge input-style
                                  {:height 46
                                   :lineHeight "46px"
                                   :fontSize 16
                                   :boxSizing "border-box"})
        drawer-item-style {:display "flex"
                           :alignItems "center"
                           :justifyContent "space-between"
                           :gap 12
                           :padding "12px 4px"
                           :borderBottom "1px solid #f0f0f0"
                           :cursor "pointer"}
        ]
    (fn []
      (let [methods (cond
                      (vector? @payment_methods) @payment_methods
                      (sequential? @payment_methods) (vec @payment_methods)
                      (map? @payment_methods) [@payment_methods]
                      :else [])
            provider-options (->> methods
                                  (keep (fn [{:keys [payment_provider_name_rus payment_provider_name is_active]}]
                                          (when (and payment_provider_name is_active)
                                            {:label (or payment_provider_name_rus payment_provider_name)
                                             :value payment_provider_name}))))
            payment-options (into [] provider-options)
            active-payment-values (->> payment-options
                                       (map :value)
                                       set)
            fallback-payment (first active-payment-values)
            selected-payment (if (contains? active-payment-values @payment_type)
                               @payment_type
                               fallback-payment)
            delivery-list (cond
                            (vector? @delivery_methods) @delivery_methods
                            (sequential? @delivery_methods) (vec @delivery_methods)
                            (map? @delivery_methods) [@delivery_methods]
                            :else [])
            delivery-options (->> delivery-list
                                  (keep (fn [{:keys [delivery_provider_name delivery_provider_name_rus is_active] :as method}]
                                          (when (and delivery_provider_name is_active)
                                            (let [price (method-default-price method)
                                                  label delivery_provider_name_rus
                                                  ]
                                              {:label label
                                               :value delivery_provider_name}))))
                                  vec)
            delivery-values (->> delivery-options
                                 (map :value)
                                 set)
            fallback-delivery (first delivery-values)
            selected-delivery (if (contains? delivery-values @delivery_provider_name)
                                @delivery_provider_name
                                fallback-delivery)
            method-by-name (into {}
                                 (map (fn [method]
                                        [(:delivery_provider_name method) method])
                                      delivery-list))
            selected-delivery-method (get method-by-name selected-delivery)
            pickup? (= "self_taken" selected-delivery)
            pvz? (contains? #{"pvz_ozon" "pvz_wb" "pvz_yandex_market"} selected-delivery)
            pvz-input-address (or (get-in @app-state [:shipping_data :pvz_address]) "")
            pickup-address (when selected-delivery-method
                             (or (method-address selected-delivery-method) ""))
            selected-delivery-price (if selected-delivery-method
                                      (method-default-price selected-delivery-method)
                                      0)
            city-value (or (get-in @app-state [:shipping_data :cdek_city]) "")
            city-code (or (get-in @app-state [:shipping_data :cdek_city_code]) "")
            cdek? (= "cdek" selected-delivery)
            tariff-options (if cdek?
                             (cdek-tariff-options selected-delivery-method)
                             [])
            selected-tariff-code (or (get-in @app-state [:shipping_data :cdek_tariff_code])
                                     (when-let [opt (first tariff-options)] (:code opt)))
            selected-tariff (some (fn [opt]
                                    (when (= (str (:code opt)) (str selected-tariff-code))
                                      opt))
                                  tariff-options)
            selected-tariff (or selected-tariff (first tariff-options))
            to-door? (= :door (:to selected-tariff))
            to-pvz? (= :pvz (:to selected-tariff))
            pvz-address-value (or (get-in @app-state [:shipping_data :cdek_pvz_address]) "")
            selected-pvz-code (get-in @app-state [:shipping_data :cdek_pvz_code])
            pvz-points @cdek-delivery-points
            pvz-query-text (or @pvz-query "")
            pvz-query-lc (-> pvz-query-text str/trim str/lower-case)
            filtered-pvz (if (str/blank? pvz-query-lc)
                           pvz-points
                           (filter (fn [point]
                                     (let [label (or (cdek-method/pvz-point-label point) "")
                                           address (or (cdek-method/pvz-point-address point) "")
                                           haystack (-> (str label " " address) str/lower-case)]
                                       (str/includes? haystack pvz-query-lc)))
                                   pvz-points))
            summary (or @cart_summary {})
            summ (or (:summ summary) 0)
            final-summ (or (:final_summ summary) summ)
            discount-value (or (:discount summary)
                               (when (not= summ final-summ) (- summ final-summ)))
            delivery-price (or @delivery_cost 0)
            total (+ final-summ delivery-price)
            errors @delivery-errors
            input-class (fn [error?]
                          (str "delivery-input" (when error? " delivery-input-error")))
            
            
            web-app (.-WebApp js/Telegram)
            platform (when web-app (.-platform web-app))
            is-tg-mobile? (contains? #{"ios" "android"} platform)
            is-mobile-device? (is-mobile?)
            is-mobile (or is-tg-mobile? is-mobile-device?)
            loading? (or (not (:payment_methods_loaded? @app-state))
                         (not (:delivery_methods_loaded? @app-state))
                         (not (:cart_summary_loaded? @app-state)))
            select-city (fn [city-name city-code]
                          (swap! app-state assoc-in [:shipping_data :cdek_city] city-name)
                          (swap! app-state assoc-in [:shipping_data :cdek_city_code] city-code)
                          (swap! app-state assoc-in [:shipping_data :address] "")
                          (swap! app-state assoc-in [:shipping_data :cdek_pvz_address] "")
                          (swap! app-state assoc-in [:shipping_data :cdek_pvz_code] nil)
                          (swap! app-state assoc-in [:shipping_data :cdek_pvz] nil)
                          (swap! app-state assoc :cdek_city_suggestions [])
                          (reset! city-drawer-open? false)
                          (when to-pvz?
                            (cdek_search_pvz city-code ""))
                          (when (and (some? city-code) (not= "" (str city-code)))
                            (cdek-method/request-cdek-calculate!
                             {:city-code city-code
                              :address ""
                              :pvz-code nil
                              :tariff-info selected-tariff})))
            select-pvz (fn [point code address]
                         (swap! app-state assoc-in [:shipping_data :cdek_pvz_code] code)
                         (swap! app-state assoc-in [:shipping_data :cdek_pvz] point)
                         (when (and (some? address) (not= "" (str address)))
                           (swap! app-state assoc-in [:shipping_data :cdek_pvz_address] address))
                         (reset! pvz-drawer-open? false)
                         (js/requestAnimationFrame
                          (fn []
                            (cdek-method/request-cdek-calculate!
                             {:city-code city-code
                              :pvz-code code
                              :tariff-info selected-tariff}))))
            focus-node! (fn [node]
                          (when node
                            (cond
                              (and (.-focus node) (fn? (.-focus node)))
                              (.focus node)
                              (and (.-input node) (.-focus (.-input node)))
                              (.focus (.-input node))
                              :else nil)))
            open-city-drawer (fn []
                               (reset! drawer-height (viewport-height))
                               (reset! city-drawer-open? true)
                               (let [current (or city-value "")]
                                 (reset! city-query current)
                                 (when (>= (count (str/trim current)) 2)
                                   (cdek_search_city current)))
                               (js/requestAnimationFrame
                                (fn []
                                  (focus-node! @city-input-ref)))
                               (js/setTimeout
                                (fn []
                                  (focus-node! @city-input-ref))
                                300))
            open-pvz-drawer (fn []
                              (reset! drawer-height (viewport-height))
                              (reset! pvz-drawer-open? true)
                              (reset! pvz-query (or pvz-address-value ""))
                              (when (and (seq (or city-code ""))
                                         (empty? @cdek-delivery-points))
                                (cdek_search_pvz city-code ""))
                              (js/requestAnimationFrame
                               (fn []
                                 (focus-node! @pvz-input-ref)))
                              (js/setTimeout
                               (fn []
                                 (focus-node! @pvz-input-ref))
                               300))
            drawer-open? (or @city-drawer-open? @pvz-drawer-open?)
            
            padding-top 87
            ]
        (set-body-lock! drawer-open?)
        (when (and selected-delivery (not= selected-delivery @delivery_provider_name))
          (swap! app-state assoc-in [:shipping_data :delivery_provider_name] selected-delivery))
        (when (and (not cdek?)
                   (not= selected-delivery-price @delivery_cost))
          (swap! app-state assoc-in [:shipping_data :delivery_cost] selected-delivery-price))
        (when (and selected-payment (not= selected-payment @payment_type))
          (swap! app-state assoc-in [:shipping_data :payment_type] selected-payment))
        (when (and cdek? selected-tariff
                   (not= (str (:code selected-tariff))
                         (str (get-in @app-state [:shipping_data :cdek_tariff_code]))))
          (swap! app-state assoc-in [:shipping_data :cdek_tariff_code] (:code selected-tariff)))
        (when (and pickup? (seq pickup-address)
                   (not= pickup-address (get-in @app-state [:shipping_data :address])))
          (swap! app-state assoc-in [:shipping_data :address] pickup-address))
        
        [:div {:style (assoc page-style
                             :paddingTop (if is-mobile (+ padding-top 24) 24)
                             :paddingBottom (if is-mobile 280 36))
               :class "delivery-page"
               }
         (when @city-drawer-open?
           [:div {:style drawer-wrap-style}
            [:div {:style drawer-mask-style
                   :onClick (fn [] (reset! city-drawer-open? false))}]
            [:div {:style (assoc drawer-sheet-style :height (if is-mobile "85%" "70%"))}
             [:div {:style {:display "flex"
                            :alignItems "center"
                            :justifyContent "space-between"
                            :padding "16px 16px 0"}}
              [:div {:style {:fontSize 18
                             :fontWeight 600
                             :color "#111111"}}
               "Поиск города"]
              [:div {:style {:width 32
                             :height 32
                             :borderRadius "50%"
                             :display "flex"
                             :alignItems "center"
                             :justifyContent "center"
                             :cursor "pointer"
                             :background "#f0f0f0"
                             :color "#6f6f6f"}
                     :onClick (fn [] (reset! city-drawer-open? false))}
               "×"]]
             [:div {:style drawer-body-style}
              [:div {:style {:fontSize 13
                             :color "#8c8c8c"
                             :lineHeight 1.4}}
               "Введите город и выберите из списка."]
              [:> Input {:style drawer-input-style
                         :inputRef (fn [node]
                                     (reset! city-input-ref node))
                         :autoFocus true
                         :autoCapitalize "none"
                         :autoCorrect "off"
                         :autoComplete "off"
                         :spellCheck false
                         :value (or @city-query "")
                         :allowClear true
                         :onChange (fn [value]
                                     (let [text (-> value .-target .-value)]
                                       (reset! city-query text)
                                       (when (>= (count (str/trim text)) 2)
                                         (cdek_search_city text))))
                         :placeholder "Город"}]
              (when @cdek-city-error
                [:div {:style {:fontSize 12
                               :color "#d4380d"}}
                 "Ошибка поиска города"])
              (when (seq @cdek-city-suggestions)
                [:div {:style {:display "flex"
                               :flexDirection "column"
                               :gap 6}}
                 (for [city (take 12 @cdek-city-suggestions)]
                   (let [city-name (:name city)
                         raw-code (:code city)
                         city-code (if (some? raw-code) (str raw-code) "")]
                     ^{:key (str city-code city-name)}
                     [:div {:style drawer-item-style
                            :onClick (fn [_]
                                       (select-city city-name city-code))}
                      [:div {:style {:display "flex"
                                     :flexDirection "column"
                                     :gap 2}}
                       [:div {:style {:fontSize 15
                                      :fontWeight 500
                                      :color "#111111"}}
                        city-name]
                       (when city-code
                         [:div {:style {:fontSize 12
                                        :color "#8c8c8c"}}
                          (str "Код: " city-code)])]
                      [:div {:style {:fontSize 18
                                     :color "#8c8c8c"}}
                       "›"]]))])]]])
         (when @pvz-drawer-open?
           [:div {:style drawer-wrap-style}
            [:div {:style drawer-mask-style
                   :onClick (fn [] (reset! pvz-drawer-open? false))}]
            [:div {:style (assoc drawer-sheet-style :height (if is-mobile "85%" "70%"))}
             [:div {:style {:display "flex"
                            :alignItems "center"
                            :justifyContent "space-between"
                            :padding "16px 16px 0"}}
              [:div {:style {:fontSize 18
                             :fontWeight 600
                             :color "#111111"}}
               "ПВЗ СДЭК"]
              [:div {:style {:width 32
                             :height 32
                             :borderRadius "50%"
                             :display "flex"
                             :alignItems "center"
                             :justifyContent "center"
                             :cursor "pointer"
                             :background "#f0f0f0"
                             :color "#6f6f6f"}
                     :onClick (fn [] (reset! pvz-drawer-open? false))}
               "×"]]
             [:div {:style drawer-body-style}
              [:div {:style {:fontSize 13
                             :color "#8c8c8c"
                             :lineHeight 1.4}}
               "Введите адрес или название ПВЗ."]
              [:> Input {:style drawer-input-style
                         :inputRef (fn [node]
                                     (reset! pvz-input-ref node))
                         :autoFocus true
                         :autoCapitalize "none"
                         :autoCorrect "off"
                         :autoComplete "off"
                         :spellCheck false
                         :value (or @pvz-query "")
                         :allowClear true
                         :onChange (fn [value]
                                     (let [text (-> value .-target .-value)]
                                       (reset! pvz-query text)))
                         :placeholder "Поиск ПВЗ"}]
              (when @cdek-delivery-points-loading?
                [:div {:style {:fontSize 12
                               :color "#8c8c8c"}}
                 "Загрузка списка ПВЗ..."])
              (when (and to-pvz? @cdek-delivery-points-error)
                [:div {:style {:fontSize 12
                               :color "#d4380d"}}
                 "СДЭК: ошибка получения ПВЗ"])
              (when (and to-pvz? (seq filtered-pvz))
                [:div {:style {:display "flex"
                               :flexDirection "column"
                               :gap 6}}
                 (for [point (take 20 filtered-pvz)]
                   (let [code (cdek-method/pvz-point-code point)
                         label (cdek-method/pvz-point-label point)
                         address (or (cdek-method/pvz-point-address point) label)]
                     ^{:key (str code)}
                     [:div {:style drawer-item-style
                            :onClick (fn [_]
                                       (select-pvz point code address))}
                      [:div {:style {:display "flex"
                                     :flexDirection "column"
                                     :gap 2}}
                       [:div {:style {:fontSize 15
                                      :fontWeight 500
                                      :color "#111111"}}
                        label]
                       (when (seq address)
                         [:div {:style {:fontSize 12
                                        :color "#8c8c8c"}}
                          address])]
                      [:div {:style {:fontSize 18
                                     :color "#8c8c8c"}}
                       "›"]]))])]]])
         (when loading?
           [:div {:style (merge container-style {:paddingTop 10})}
            [:div {:style {:height 20
                           :width "55%"
                           :borderRadius 4
                           :background "#f0f0f0"}}]
            (for [idx (range 4)]
              ^{:key (str "delivery-input-skel-" idx)}
              [:div {:style {:height 46
                             :width "100%"
                             :borderRadius 6
                             :background "#f0f0f0"}}])
            [:div {:style {:height 20
                           :width "40%"
                           :borderRadius 4
                           :background "#f0f0f0"
                           :marginTop 8}}]
            (for [idx (range 2)]
              ^{:key (str "delivery-radio-skel-" idx)}
              [:div {:style {:height 18
                             :width "70%"
                             :borderRadius 4
                             :background "#f0f0f0"}}])
            [:div {:style {:height 46
                           :width "100%"
                           :borderRadius 6
                           :background "#f0f0f0"
                           :marginTop 8}}]
            [:div {:style {:height 20
                           :width "40%"
                           :borderRadius 4
                           :background "#f0f0f0"
                           :marginTop 12}}]
            (for [idx (range 2)]
              ^{:key (str "payment-radio-skel-" idx)}
              [:div {:style {:height 18
                             :width "70%"
                             :borderRadius 4
                             :background "#f0f0f0"}}])
            [:div {:style {:marginTop 16
                           :display "flex"
                           :flexDirection "column"
                           :alignItems "flex-end"
                           :gap 8}}
             [:div {:style {:height 14
                            :width 120
                            :borderRadius 4
                            :background "#f0f0f0"}}]
             [:div {:style {:height 14
                            :width 140
                            :borderRadius 4
                            :background "#f0f0f0"}}]
             [:div {:style {:height 20
                            :width 160
                            :borderRadius 4
                            :background "#f0f0f0"}}]]]
                            )
          
          (when (not loading?)
           [:div {:style container-style}
            [:div {:style section-title-style}
             "Личные данные"]
            [:div {:style section-style}
             [:div {:style {:display "flex"
                            :flexDirection "column"
                            :gap 6}}
              [:> Input {:style input-style
                         :className (input-class (contains? errors :surname))
                         :id "delivery-surname"
                         :defaultValue (get-in @app-state [:shipping_data :surname])
                         :allowClear true
                         :onChange (fn [value]
                                     (let [field-value (-> value .-target .-value)]
                                       (swap! app-state assoc-in [:shipping_data :surname] field-value)
                                       (when (and (contains? errors :surname)
                                                  (not (str/blank? field-value)))
                                         (swap! app-state update :delivery_errors dissoc :surname))))
                         :placeholder "Фамилия"}]
              
              
              (when-let [error-text (:surname errors)]
                [:div {:style {:fontSize 12
                               :color "#e03a3a"}}
                 error-text])
              ]

             [:div {:style {:display "flex"
                            :flexDirection "column"
                            :gap 6}}
              [:> Input {:style input-style
                         :className (input-class (contains? errors :first_name))
                         :id "delivery-first-name"
                         :defaultValue (get-in @app-state [:shipping_data :first_name])
                         :allowClear true
                         :onChange (fn [value]
                                     (let [field-value (-> value .-target .-value)]
                                       (swap! app-state assoc-in [:shipping_data :first_name] field-value)
                                       (when (and (contains? errors :first_name)
                                                  (not (str/blank? field-value)))
                                         (swap! app-state update :delivery_errors dissoc :first_name))))
                         :placeholder "Имя"}]
              (when-let [error-text (:first_name errors)]
                [:div {:style {:fontSize 12
                               :color "#e03a3a"}}
                 error-text])
              
              ]

             [:div {:style {:display "flex"
                            :flexDirection "column"
                            :gap 6}}
              [:> Input {:style input-style
                         :className "delivery-input"
                         :defaultValue (get-in @app-state [:shipping_data :patronymic])
                         :allowClear true
                         :onChange (fn [value]
                                     (swap! app-state assoc-in [:shipping_data :patronymic] (-> value .-target .-value)))
                         :placeholder "Отчество"}]]

             [:div {:style {:display "flex"
                            :flexDirection "column"
                            :gap 6}}
              [:> Input {:style input-style
                         :className (input-class (contains? errors :mail))
                         :id "delivery-mail"
                         :defaultValue (get-in @app-state [:shipping_data :mail])
                         :allowClear true
                         :onChange (fn [value]
                                     (let [field-value (-> value .-target .-value)
                                           email-value (or field-value "")
                                           email-valid? (re-matches #"^[^\s@]+@[^\s@]+(\.[^\s@]+)*$" email-value)]
                                       (swap! app-state assoc-in [:shipping_data :mail] field-value)
                                       (when (and (contains? errors :mail) email-valid?)
                                         (swap! app-state update :delivery_errors dissoc :mail))))
                         :placeholder "Email"}]
              (when-let [error-text (:mail errors)]
                [:div {:style {:fontSize 12
                               :color "#e03a3a"}}
                 error-text]
                )
              ]

             [:div {:style {:display "flex"
                            :flexDirection "column"
                            :gap 6}}
              [phone_input input-style
               (input-class (contains? errors :phone))
               "delivery-phone"
               delivery-errors]
              (when-let [error-text (:phone errors)]
                [:div {:style {:fontSize 12
                               :color "#e03a3a"}}
                 error-text]
                )
              ]
             ]

            [:div {:style section-title-style}
             "Доставка"]
            
            [:div {:style section-style}
             (when (seq delivery-options)
               [:> RadioGroup {:value selected-delivery
                               :class-name "delivery-page"
                               :style radio-group-style
                               :onChange (fn [e]
                                           (let [value (.. e -target -value)
                                                 method (get method-by-name value)
                                                 price (if method (method-default-price method) 0)
                                                 tariffs (if (= value "cdek") (cdek-tariff-options method) [])
                                                 tariff-code (or (when-let [opt (first tariffs)] (:code opt))
                                                                 (method-tariff-code method))]
                                             (swap! app-state assoc-in [:shipping_data :delivery_provider_name] value)
                                             (swap! app-state assoc-in [:shipping_data :delivery_cost] (if (= value "cdek") 0 price))
                                             (swap! app-state assoc-in [:shipping_data :cdek_tariff_code] tariff-code)
                                             (when (= value "cdek")
                                               (swap! app-state assoc-in [:shipping_data :address] "")
                                               (swap! app-state assoc-in [:shipping_data :cdek_pvz_address] "")
                                               (swap! app-state assoc-in [:shipping_data :cdek_pvz_code] nil)
                                               (swap! app-state assoc-in [:shipping_data :cdek_pvz] nil)
                                               (swap! app-state assoc :cdek_calculate_error nil))
                                             (when (not= value "cdek")
                                               (swap! app-state assoc
                                                      :cdek_delivery_points []
                                                      :cdek_delivery_points_loading? false
                                                      :cdek_delivery_points_error nil)
                                               (swap! app-state assoc-in [:shipping_data :cdek_pvz_code] nil)
                                               (swap! app-state assoc-in [:shipping_data :cdek_pvz] nil)
                                               (swap! app-state assoc-in [:shipping_data :cdek_pvz_address] ""))
                                             (when-not (contains? #{"pvz_ozon" "pvz_wb" "pvz_yandex_market"} value)
                                               (swap! app-state assoc-in [:shipping_data :pvz_address] ""))))
                               :options delivery-options}
                               ]
                               )
                               ]
             
             (cond
              pickup? [self-taken-method/self-taken-block pickup-address]
              pvz?    [:div {:style {:display "flex" :flexDirection "column" :gap 6}}
                       [:div {:style hint-text-style}
                        "Введите адрес ближайшего ПВЗ в свободной форме."]
                       [:> Input {:style input-style
                                  :className "delivery-input"
                                  :value pvz-input-address
                                  :allowClear true
                                  :onChange (fn [event]
                                              (swap! app-state assoc-in [:shipping_data :pvz_address]
                                                     (-> event .-target .-value)))
                                  :placeholder "Адрес ПВЗ"}]]
              :else [:<>
                (when cdek?
                  [:<>
                   (when (seq tariff-options)
                     [:div {:style {:display "flex"
                                    :flexDirection "column"
                                    :gap 6}}
                      [:div {:style {:fontSize 12
                                     :color "#8c8c8c"}}
                       "Тариф СДЭК"]
                      [:> RadioGroup {:value (when selected-tariff (str (:code selected-tariff)))
                                      :onChange (fn [e]
                                                  (let [value (.. e -target -value)
                                                        picked (some (fn [opt]
                                                                       (when (= (str (:code opt)) (str value))
                                                                         opt))
                                                                     tariff-options)
                                                        to-type (:to picked)
                                                        current-city (get-in @app-state [:shipping_data :cdek_city_code])
                                                        door-address (get-in @app-state [:shipping_data :address])
                                                        pvz-code (get-in @app-state [:shipping_data :cdek_pvz_code])]
                                                    (swap! app-state assoc-in [:shipping_data :cdek_tariff_code] value)
                                                    (swap! app-state assoc-in [:shipping_data :delivery_cost] 0)
                                                    (swap! app-state assoc :cdek_calculate_error nil)
                                                    (when (and (= to-type :pvz)
                                                               (some? current-city)
                                                               (not= "" (str current-city)))
                                                      (cdek_search_pvz current-city ""))
                                                    (when (and (some? current-city)
                                                               (not= "" (str current-city)))
                                                      (cdek-method/request-cdek-calculate!
                                                       {:city-code current-city
                                                        :address door-address
                                                        :pvz-code pvz-code
                                                        :tariff-info picked}))))
                                      :style {:display "flex"
                                              :flexDirection "column"
                                              :gap 6}
                                      :options (mapv (fn [opt]
                                                       {:label (:label opt)
                                                        :value (str (:code opt))})
                                                     tariff-options)}]])
                   [:div {:onClick (fn [_]
                                     (open-city-drawer))}
                    [:> Input {:style input-style
                               :className "delivery-input"
                               :value (or city-value "")
                               :readOnly true
                               :placeholder "Город"}]]
                   [:div {:style hint-text-style}
                    "Введите город полностью, например: Москва."]]
                    )
                
                (when to-door?
                  [:> Input {:style input-style
                             :className "delivery-input"
                             :value (get-in @app-state [:shipping_data :address]) 
                             :allowClear true
                             :onChange (fn [value] 
                                         (let [address-text (-> value .-target .-value)]
                                           (swap! app-state assoc-in [:shipping_data :address] address-text)
                                           (when (and cdek? to-door?
                                                      (>= (count (or address-text "")) 3)
                                                      (some? city-code)
                                                      (not= "" (str city-code)))
                                             (cdek-method/request-cdek-calculate!
                                              {:city-code city-code
                                               :address address-text
                                               :tariff-info selected-tariff}))))
                             :placeholder "Адрес"}])
                (when to-pvz?
                  [:div {:onClick (fn [_]
                                    (open-pvz-drawer))}
                   [:> Input {:style input-style
                              :className "delivery-input"
                              :value pvz-address-value
                              :readOnly true
                              :placeholder "Адрес ПВЗ"}]])
                (when (and to-pvz? (seq selected-pvz-code))
                  [:div {:style {:fontSize 12
                                 :color "#4c4c4c"}}
                   (str "Выбранный ПВЗ: " selected-pvz-code)])
                ]
                )
             [:> Input {:style input-style
                        :class-name "delivery-page"
                        :defaultValue (get-in @app-state [:shipping_data :comment])
                        :allowClear true
                        :onChange (fn [value]
                                    (swap! app-state assoc-in [:shipping_data :comment] (-> value .-target .-value)))
                        :placeholder "Комментарий к заказу"}]

            [:div {:style section-title-style}
             "Оплата"]
            (when (seq payment-options)
              [:> RadioGroup {:className "delivery-page"
                              :value selected-payment
                              :onChange (fn [e]
                                          (let [value (.. e -target -value)]
                                            (swap! app-state assoc-in [:shipping_data :payment_type] value)))
                              :style radio-group-style
                              :options payment-options}])

            [:div
             [:div {:style summary-style}
              [:div {:style summary-line-style}
               (str "Сумма: " summ "₽")]
              (when discount-value
                [:div {:style summary-line-style}
                 (str "Скидка: " discount-value "₽")])
              [:div {:style summary-line-style}
               (str "Доставка: " delivery-price "₽")]]

             [:div {:style total-style}
              "Сумма к оплате: "
              [:span {:style {:color "var(--color-accent)"}}
               (str total "₽")]]
            ]
            ]
            
            )
            ]
            )
            )
            )
            )
