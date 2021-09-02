package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.CustomText.Restrictions
import org.wordpress.android.fluxc.domain.Addon.TitleFormat
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalDisplay.Images
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalDisplay.RadioButton
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalDisplay.Select
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalPriceType.FlatFee
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalPriceType.PercentageBased
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalPriceType.QuantityBased
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalRestrictions.AnyText
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalRestrictions.Email
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalRestrictions.OnlyLetters
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalRestrictions.OnlyLettersNumbers
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalRestrictions.OnlyNumbers
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalTitleFormat.Hide
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalTitleFormat.Label
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.Checkbox
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.CustomPrice
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.CustomText
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.CustomTextArea
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.FileUpload
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.Heading
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.InputMultiplier
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.MultipleChoice
import org.wordpress.android.fluxc.persistence.entity.AddonWithOptions

object FromDatabaseAddonsMapper {
    fun toDomainModel(entity: AddonWithOptions): Addon {
        return when (entity.addon.type) {
            MultipleChoice -> Addon.MultipleChoice(
                    name = entity.addon.name,
                    titleFormat = entity.addon.titleFormat.toDomainModel(),
                    description = entity.addon.description,
                    required = entity.addon.required,
                    position = entity.addon.position,
                    options = entity.mapOptions(),
                    display = entity.addon.display?.toDomainModel() ?: throw Exception()
            )
            Checkbox -> Addon.Checkbox(
                    name = entity.addon.name,
                    titleFormat = entity.addon.titleFormat.toDomainModel(),
                    description = entity.addon.description,
                    required = entity.addon.required,
                    position = entity.addon.position,
                    options = entity.mapOptions()
            )
            CustomText -> Addon.CustomText(
                    name = entity.addon.name,
                    titleFormat = entity.addon.titleFormat.toDomainModel(),
                    description = entity.addon.description,
                    required = entity.addon.required,
                    position = entity.addon.position,
                    restrictions = entity.addon.mapRestrictions(),
                    price = entity.addon.mapPrice(),
                    characterLength = if (entity.addon.max == null || entity.addon.max == 0L) null else
                        ((entity.addon.min ?: 0)..entity.addon.max)
            )
            CustomTextArea -> Addon.CustomTextArea(
                    name = entity.addon.name,
                    titleFormat = entity.addon.titleFormat.toDomainModel(),
                    description = entity.addon.description,
                    required = entity.addon.required,
                    position = entity.addon.position,
                    price = entity.addon.mapPrice(),
                    characterLength = if (entity.addon.max == null || entity.addon.max == 0L) null else
                        ((entity.addon.min ?: 0)..entity.addon.max)
            )
            FileUpload -> Addon.FileUpload(
                    name = entity.addon.name,
                    titleFormat = entity.addon.titleFormat.toDomainModel(),
                    description = entity.addon.description,
                    required = entity.addon.required,
                    position = entity.addon.position,
                    price = entity.addon.mapPrice()
            )
            CustomPrice -> Addon.CustomPrice(
                    name = entity.addon.name,
                    titleFormat = entity.addon.titleFormat.toDomainModel(),
                    description = entity.addon.description,
                    required = entity.addon.required,
                    position = entity.addon.position,
                    priceRange = if (entity.addon.max == null || entity.addon.max == 0L) null else ((entity.addon.min
                            ?: 0)..entity.addon.max)
            )
            InputMultiplier -> Addon.InputMultiplier(
                    name = entity.addon.name,
                    titleFormat = entity.addon.titleFormat.toDomainModel(),
                    description = entity.addon.description,
                    required = entity.addon.required,
                    position = entity.addon.position,
                    price = entity.addon.mapPrice(),
                    quantityRange =
                    if (entity.addon.max == null || entity.addon.max == 0L) null else ((entity.addon.min
                            ?: 0)..entity.addon.max)
            )
            Heading -> Addon.Heading(
                    name = entity.addon.name,
                    titleFormat = entity.addon.titleFormat.toDomainModel(),
                    description = entity.addon.description,
                    required = entity.addon.required,
                    position = entity.addon.position
            )
        }
    }

    private fun AddonWithOptions.mapOptions(): List<Addon.HasOptions.Option> {
        return this.options.map { optionEntity ->
            DatabaseAddonOptionMapper.toDomain(optionEntity)
        }
    }

    private fun AddonEntity.mapRestrictions(): Restrictions {
        return if (restrictions != null) {
            when (this.restrictions) {
                AnyText -> Restrictions.AnyText
                OnlyLetters -> Restrictions.OnlyLetters
                OnlyNumbers -> Restrictions.OnlyNumbers
                OnlyLettersNumbers -> Restrictions.OnlyLettersNumbers
                Email -> Restrictions.Email
            }
        } else {
            throw Exception()
        }
    }

    private fun AddonEntity.mapPrice(): Addon.HasAdjustablePrice.Price {
        return if (this.priceType != null) {
            Addon.HasAdjustablePrice.Price.Adjusted(
                    priceType = this.priceType.mapToDomain(),
                    value = this.price.orEmpty()
            )
        } else {
            Addon.HasAdjustablePrice.Price.NotAdjusted
        }
    }

    private fun AddonEntity.LocalTitleFormat.toDomainModel(): TitleFormat {
        return when (this) {
            Label -> TitleFormat.Label
            AddonEntity.LocalTitleFormat.Heading -> TitleFormat.Heading
            Hide -> TitleFormat.Hide
        }
    }

    private fun AddonEntity.LocalDisplay.toDomainModel(): Addon.MultipleChoice.Display {
        return when (this) {
            Select -> Addon.MultipleChoice.Display.Select
            RadioButton -> Addon.MultipleChoice.Display.RadioButton
            Images -> Addon.MultipleChoice.Display.Images
        }
    }

    fun AddonEntity.LocalPriceType.mapToDomain(): Addon.HasAdjustablePrice.Price.Adjusted.PriceType {
        return when (this) {
            FlatFee -> Addon.HasAdjustablePrice.Price.Adjusted.PriceType.FlatFee
            QuantityBased -> Addon.HasAdjustablePrice.Price.Adjusted.PriceType.QuantityBased
            PercentageBased -> Addon.HasAdjustablePrice.Price.Adjusted.PriceType.PercentageBased
        }
    }
}
