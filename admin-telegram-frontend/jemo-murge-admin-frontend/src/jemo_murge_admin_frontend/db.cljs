(ns jemo-murge-admin-frontend.db
  (:require
   [reagent.core :as r]
   )
  )

(defonce app-state (r/atom {:login? true 
                            :units []
                            :model_unit []
                            :page :units
                            :show_modal_banner_edit? false
                            :show_modal_product_add? false
                            :product_editing? false
                            :selected_units_catalog []
                            :selected_units_model_unit []
                            :filters_picked {}
                            :products_mode "catalog"
                            :model_unit_mode "catalog"
                            :product_changes {}
                            :product_draft {}
                            :product_server {}
                            :scroll_restore_y nil
                            :adding_new_article? false
                            :adding_new_product? false
                            :show_model_unit_editing? false
                            :show_modal_fill_analogy? false
                            :model_unit_attribute_name "vendor_code"
                            :current_vendor_changes {}
                            :options_to_fill []
                            :banner_edit_mode false
                            :banners []
                            :banner_images_edit []
                            :second_lvl_config {}
                            :orders_history []
                            :managers []
                            :owners []
                            :managers_request []
                            :settings {}
                            :delivery_methods []
                            :current_delivery_provider_name nil

                            :modal_tutorial_open? false
                            :tour_open? false
                            :tour_current 0
                            :tour_completed false

                            :units_tour_open? true
                            :units_tour_current 0
                            :units_tour_completed false
                            }
                            )
                            )
