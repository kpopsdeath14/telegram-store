(ns {{name}}.pages.delivery.methods.cdek
  (:require
   [clojure.string :as str]
   [{{name}}.db :refer [app-state]]
   [{{name}}.events.cdek-calculate :refer [cdek_calculate]]
   [reagent.core :as reagent]))

(defn pvz-point-code [point]
  (or (:code point)
      (get point "code")
      (:point_code point)
      (get point "point_code")))

(defn pvz-point-address [point]
  (or (get-in point [:address :address])
      (get-in point ["address" "address"])
      (:address point)
      (get point "address")
      (:full_address point)
      (get point "full_address")
      (get-in point [:location :address])
      (get-in point ["location" "address"])))

(defn pvz-point-name [point]
  (or (:name point)
      (get point "name")
      (pvz-point-code point)))

(defn pvz-point-label [point]
  (let [code (pvz-point-code point)
        name (pvz-point-name point)
        address (pvz-point-address point)]
    (str (or name "")
         (when (and name address) " — ")
         (or address "")
         (when code (str " (" code ")")))))

(defn safe-string [value]
  (cond
    (string? value) value
    (nil? value) ""
    :else (str value)))

(defn normalize-search [value]
  (-> (safe-string value)
      str/lower-case
      (str/replace #"[\\s\\-–—_.,()]+"
                   "")))

(defn pvz-point-search-text [point]
  (->> [(pvz-point-label point)
        (pvz-point-address point)
        (pvz-point-name point)
        (pvz-point-code point)
        (:address_full point)
        (get point "address_full")
        (:address_comment point)
        (get point "address_comment")
        (get-in point [:location :address_full])
        (get-in point ["location" "address_full"])]
       (map safe-string)
       (remove str/blank?)
       (str/join " ")))

(defn parse-number [value]
  (cond
    (number? value) value
    (string? value) (let [parsed (js/parseFloat value)]
                      (when-not (js/isNaN parsed) parsed))
    :else nil))

(defn extract-tariff-items [body]
  (let [payload (if (map? body) body {})
        items (or (:tariff_codes payload)
                  (get payload "tariff_codes")
                  (:tariffs payload)
                  (get payload "tariffs")
                  (when (sequential? body) body)
                  [])]
    (cond
      (vector? items) items
      (sequential? items) (vec items)
      (map? items) [items]
      :else [])))

(defn find-tariff [items tariff-code]
  (let [code-str (when (some? tariff-code) (str tariff-code))]
    (if code-str
      (some (fn [item]
              (let [item-code (or (:tariff_code item)
                                  (get item "tariff_code")
                                  (:code item)
                                  (get item "code"))]
                (when (= (str item-code) code-str)
                  item)))
            items)
      (first items))))

(defn tariff-price [tariff]
  (or (parse-number (:delivery_sum tariff))
      (parse-number (get tariff "delivery_sum"))
      (parse-number (:delivery_sum_with_discount tariff))
      (parse-number (get tariff "delivery_sum_with_discount"))
      (parse-number (:price tariff))
      (parse-number (get tariff "price"))))

(defn cart-summary->packages [summary]
  (when (map? summary)
    (let [weight (or (parse-number (:weight summary))
                     (parse-number (get summary "weight"))
                     (parse-number (:total_weight summary))
                     (parse-number (get summary "total_weight"))
                     (parse-number (:weight_total summary))
                     (parse-number (get summary "weight_total")))]
      (when weight
        [{:weight weight}]))))

(defn base-cdek-request [state]
  (let [shipping (:shipping_data state)
        settings (:settings state)
        from-shipping (or (:cdek_request shipping)
                          (get shipping "cdek_request")
                          (:cdek_calculate_request shipping)
                          (get shipping "cdek_calculate_request"))
        from-settings (or (:cdek_request settings)
                          (get settings "cdek_request")
                          (:cdek_calculate_request settings)
                          (get settings "cdek_calculate_request"))]
    (cond
      (map? from-shipping) from-shipping
      (map? from-settings) from-settings
      :else {})))

(defn build-cdek-request [state {:keys [city-code address pvz-code tariff-info]}]
  (let [base (base-cdek-request state)
        tariff-code (:code tariff-info)
        from-type (:from tariff-info)
        to-type (:to tariff-info)
        from-address (or (:from_address tariff-info) (get tariff-info "from_address"))
        from-pvz-code (or (:from_pvz_code tariff-info) (get tariff-info "from_pvz_code"))
        from-city-code (or (:from_city_code tariff-info) (get tariff-info "from_city_code"))
        existing-to (or (:to_location base) (get base "to_location") {})
        existing-from (or (:from_location base) (get base "from_location") {})
        computed-to (cond
                      (= to-type :door)
                      (let [addr (safe-string address)
                            code (when (and (some? city-code) (not= "" (str city-code)))
                                   (str city-code))]
                        (cond
                          (not= "" addr) (cond-> {:address addr}
                                           code (assoc :code code))
                          code {:code code}
                          :else nil))
                      (= to-type :pvz)
                      (when (and (some? city-code) (not= "" (str city-code)))
                        {:code (str city-code)})
                      :else nil)
        computed-from (cond
                        (= from-type :door)
                        (let [addr (safe-string from-address)
                              code (when (and (some? from-city-code) (not= "" (str from-city-code)))
                                     (str from-city-code))]
                          (cond
                            (not= "" addr) (cond-> {:address addr}
                                             code (assoc :code code))
                            code {:code code}
                            :else nil))
                        (= from-type :pvz)
                        (let [code (when (and (some? from-city-code) (not= "" (str from-city-code)))
                                     (str from-city-code))]
                          (when (some? code)
                            {:code code}))
                        :else nil)
        to-location (merge (if (map? existing-to) existing-to {}) (if (map? computed-to) computed-to {}))
        from-location (merge (if (map? existing-from) existing-from {}) (if (map? computed-from) computed-from {}))
        packages (or (:packages base)
                     (get base "packages")
                     (cart-summary->packages (:cart_summary state)))]
    (cond-> base
      ;; TO: только одно поле - либо delivery_point (для ПВЗ), либо to_location (для двери)
      (and (= to-type :pvz) (some? pvz-code) (not= "" (str pvz-code)))
      (assoc :delivery_point (str pvz-code))

      (and (= to-type :door) (seq to-location))
      (assoc :to_location to-location)

      ;; FROM: только одно поле - либо shipment_point (для ПВЗ), либо from_location (для двери)
      (and (= from-type :pvz) (some? from-pvz-code) (not= "" (str from-pvz-code)))
      (assoc :shipment_point (str from-pvz-code))

      (and (= from-type :door) (seq from-location))
      (assoc :from_location from-location)

      (seq packages) (assoc :packages packages)
      (some? tariff-code) (assoc :tariff_code tariff-code))))

(defn request-cdek-calculate! [{:keys [city-code address pvz-code tariff-info]}]
  (let [request (build-cdek-request @app-state {:city-code city-code
                                                :address address
                                                :pvz-code pvz-code
                                                :tariff-info tariff-info})
        tariff-code (:code tariff-info)]
    (if (seq request)
      (do
        (swap! app-state assoc :cdek_calculate_error nil)
        (cdek_calculate {:cdek_request request}
                        (fn [response]
                          (let [body (or (:body response) (get response "body") response)
                                items (extract-tariff-items body)
                                matched (find-tariff items tariff-code)
                                price (tariff-price matched)]
                            (if price
                              (do
                                (swap! app-state assoc-in [:shipping_data :delivery_cost] price)
                                (swap! app-state assoc :cdek_calculate_error nil))
                              (do
                                (swap! app-state assoc :cdek_calculate_error body)
                                nil))))
                        (fn [err]
                          (swap! app-state assoc :cdek_calculate_error err))))
      (swap! app-state assoc :cdek_calculate_error {:reason :empty_request
                                                    :tariff_info tariff-info
                                                    :city_code city-code
                                                    :address address
                                                    :pvz_code pvz-code}))))

(defn cdek-controls [{:keys [cdek? address-text tariff-info]}]
  (let [pvz-points (reagent/cursor app-state [:cdek_delivery_points])
        pvz-loading? (reagent/cursor app-state [:cdek_delivery_points_loading?])
        pvz-error (reagent/cursor app-state [:cdek_delivery_points_error])
        selected-pvz-code (reagent/cursor app-state [:shipping_data :cdek_pvz_code])
        city-code (get-in @app-state [:shipping_data :cdek_city_code])
        tariff-info (or tariff-info {:to :pvz})
        tariff-code (:code tariff-info)
        to-pvz? (= :pvz (:to tariff-info))
        address-query (-> (safe-string address-text) str/trim)
        normalized-query (normalize-search address-query)
        filter-on? (>= (count normalized-query) 2)
        points @pvz-points
        filtered (if filter-on?
                   (filter (fn [point]
                             (let [haystack (-> (pvz-point-search-text point)
                                                normalize-search)]
                               (str/includes? haystack normalized-query)))
                           points)
                   points)
        filtered-points filtered]
    (when cdek?
      [:div {:style {:display "flex"
                     :flexDirection "column"
                     :gap 6
                     :marginTop 8}}
       (when (and to-pvz? @pvz-loading?)
         [:div {:style {:fontSize 12
                        :color "#8c8c8c"}}
          "Загрузка списка ПВЗ..."])
       (when (and to-pvz? @pvz-error)
         [:div {:style {:fontSize 12
                        :color "#d4380d"}}
          "СДЭК: ошибка получения ПВЗ"])
       (when (and to-pvz? (seq filtered-points))
         [:div {:style {:display "flex"
                        :flexDirection "column"
                        :gap 8
                        :maxHeight 240
                        :overflowY "auto"
                        :padding 4
                        :border "1px solid #e5e5e5"
                        :borderRadius 12}}
          (for [point (take 10 filtered-points)]
            (let [code (pvz-point-code point)
                  label (pvz-point-label point)
                  address (or (pvz-point-address point) label)
                  selected? (= (str code) (str @selected-pvz-code))]
              ^{:key (str code)}
              [:div {:style (merge {:padding "10px 12px"
                                    :border "1px solid #d9d9d9"
                                    :borderRadius 12
                                    :cursor "pointer"
                                    :background (if selected?
                                                  "#f0f7ff"
                                                  "#ffffff")}
                                   (when selected?
                                     {:borderColor "#1677ff"}))
                     :onClick (fn []
                                (swap! app-state assoc-in [:shipping_data :cdek_pvz_code] code)
                                (swap! app-state assoc-in [:shipping_data :cdek_pvz] point)
                                (when (and (some? address) (not= "" (str address)))
                                  (swap! app-state assoc-in [:shipping_data :cdek_pvz_address] address))
                                (request-cdek-calculate! {:city-code city-code
                                                          :pvz-code code
                                                          :tariff-info tariff-info}))}
               [:div {:style {:fontSize 13
                              :fontWeight 500}}
                label]]))])
       (when (and to-pvz? (seq @selected-pvz-code))
         [:div {:style {:fontSize 12
                        :color "#4c4c4c"}}
          (str "Выбранный ПВЗ: " @selected-pvz-code)])])))
