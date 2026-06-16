(ns {{name}}.db
  (:require
   [reagent.core :as r]
   )
  )

(defonce app-state (r/atom {:production true
                            :page :catalog 
                            :side_menu_open false
                            :products [:empty]
                            :products_loaded? false
                            :banners []
                            :banners_loaded? false
                            :filters_picked {}
                            :filters []
                            :filters_loaded? false
                            :product_current {}
                            :cart [{} {} {}]
                            :cart_stock_issues {:show? false
                                                :items #{}} 
                            :cart_pending_ids #{}
                            :cart_loaded? false
                            :cart_summary_loaded? false
                            :current_vendor_code nil
                            :current_color nil
                            :policies_all []
                            :unit {}
                            :prev_page :catalog
                            :orders_history []
                            :orders_history_loaded? false
                            :order_loaded? false
                            :order_current {}
                            :filters_menu_open false
                            :sort_menu_open false
                            :selected_sorting "default"
                            :policies_checked []
                            :policies_menu_open? false
                            :delivery_errors {}
                            :delivery_methods []
                            :cdek_delivery_points []
                            :cdek_delivery_points_loading? false
                            :cdek_delivery_points_error nil
                            :cdek_city_suggestions []
                            :cdek_city_loading? false
                            :cdek_city_error nil
                            :cdek_calculate_error nil
                            :payment_methods_loaded? false
                            :delivery_methods_loaded? false
                            :settings_loaded? false
                            :catalog_config_loaded? false
                            :app_ready? false
                            :favorites []
                            :favorites_loaded? false
                            :search_value ""
                            :current_order_id nil
                            :current_payment_id nil
                            :last_paid_order_id nil
                            :user_status nil
                            :app_state nil
                            :technical_work? false
                            }
                           )
                           )
