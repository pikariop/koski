import { todayISODate } from '../../date/date'
import { Arviointi } from '../../types/fi/oph/koski/schema/Arviointi'
import { MuuallaSuoritettuOppivelvollisilleSuunnatunVapaanSivistystyönOpintojenSuoritus } from '../../types/fi/oph/koski/schema/MuuallaSuoritettuOppivelvollisilleSuunnatunVapaanSivistystyonOpintojenSuoritus'
import { OppivelvollisilleSuunnatunVapaanSivistystyönOpintokokonaisuudenSuoritus } from '../../types/fi/oph/koski/schema/OppivelvollisilleSuunnatunVapaanSivistystyonOpintokokonaisuudenSuoritus'
import { VSTKotoutumiskoulutuksenKieliJaViestintäosaamisenOsasuoritus } from '../../types/fi/oph/koski/schema/VSTKotoutumiskoulutuksenKieliJaViestintaosaamisenOsasuoritus'
import { VSTKotoutumiskoulutuksenKieliJaViestintäosaamisenSuoritus2022 } from '../../types/fi/oph/koski/schema/VSTKotoutumiskoulutuksenKieliJaViestintaosaamisenSuoritus2022'
import { VSTKotoutumiskoulutuksenValinnaistenOpintojenOsasuoritus2022 } from '../../types/fi/oph/koski/schema/VSTKotoutumiskoulutuksenValinnaistenOpintojenOsasuoritus2022'
import { VSTKotoutumiskoulutuksenYhteiskuntaJaTyöelämäosaaminenSuoritus2022 } from '../../types/fi/oph/koski/schema/VSTKotoutumiskoulutuksenYhteiskuntaJaTyoelamaosaaminenSuoritus2022'
import { VapaanSivistystyönJotpaKoulutuksenOsasuorituksenSuoritus } from '../../types/fi/oph/koski/schema/VapaanSivistystyonJotpaKoulutuksenOsasuorituksenSuoritus'
import { VapaanSivistystyönLukutaitokoulutuksenKokonaisuudenSuoritus } from '../../types/fi/oph/koski/schema/VapaanSivistystyonLukutaitokoulutuksenKokonaisuudenSuoritus'
import { VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenKieliopintojenSuoritus } from '../../types/fi/oph/koski/schema/VapaanSivistystyonMaahanmuuttajienKotoutumiskoulutuksenKieliopintojenSuoritus'
import { VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenOhjauksenSuoritus } from '../../types/fi/oph/koski/schema/VapaanSivistystyonMaahanmuuttajienKotoutumiskoulutuksenOhjauksenSuoritus'
import { VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenTyöelämäJaYhteiskuntataitojenOpintojenSuoritus } from '../../types/fi/oph/koski/schema/VapaanSivistystyonMaahanmuuttajienKotoutumiskoulutuksenTyoelamaJaYhteiskuntataitojenOpintojenSuoritus'
import { VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenTyöelämäJaYhteiskuntataitojenTyöelämäJakso } from '../../types/fi/oph/koski/schema/VapaanSivistystyonMaahanmuuttajienKotoutumiskoulutuksenTyoelamaJaYhteiskuntataitojenTyoelamaJakso'
import { VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenValinnaistenOpintojenOsasuoritus } from '../../types/fi/oph/koski/schema/VapaanSivistystyonMaahanmuuttajienKotoutumiskoulutuksenValinnaistenOpintojenOsasuoritus'
import { VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenValinnaistenOpintojenSuoritus } from '../../types/fi/oph/koski/schema/VapaanSivistystyonMaahanmuuttajienKotoutumiskoulutuksenValinnaistenOpintojenSuoritus'
import { VapaanSivistystyönOpiskeluoikeus } from '../../types/fi/oph/koski/schema/VapaanSivistystyonOpiskeluoikeus'
import { VapaanSivistystyönVapaatavoitteisenKoulutuksenOsasuorituksenSuoritus } from '../../types/fi/oph/koski/schema/VapaanSivistystyonVapaatavoitteisenKoulutuksenOsasuorituksenSuoritus'
import { VSTSuoritus, VSTSuoritusArvioinnilla } from './types'

export const createArviointi =
  <T extends Arviointi>(
    ctor: (p: { arvosana: T['arvosana']; päivä: string }) => T
  ) =>
  (arvosana: T['arvosana']) =>
    ctor({
      arvosana,
      päivä: todayISODate()
    })

export const kaikkiOsasuorituksetVahvistettu = (
  oo: VapaanSivistystyönOpiskeluoikeus
): boolean =>
  oo.suoritukset
    .flatMap<any>((s) => s.osasuoritukset || [])
    .filter(isVSTOsasuoritusArvioinnilla)
    .filter((os) => os.arviointi === undefined).length === 0

export function isVSTOsasuoritusArvioinnilla(
  s: VSTSuoritus
): s is VSTSuoritusArvioinnilla {
  switch (s.$class) {
    case MuuallaSuoritettuOppivelvollisilleSuunnatunVapaanSivistystyönOpintojenSuoritus.className:
    case OppivelvollisilleSuunnatunVapaanSivistystyönOpintokokonaisuudenSuoritus.className:
    case VSTKotoutumiskoulutuksenKieliJaViestintäosaamisenOsasuoritus.className:
    case VSTKotoutumiskoulutuksenKieliJaViestintäosaamisenSuoritus2022.className:
    case VSTKotoutumiskoulutuksenValinnaistenOpintojenOsasuoritus2022.className:
    case VSTKotoutumiskoulutuksenYhteiskuntaJaTyöelämäosaaminenSuoritus2022.className:
    case VapaanSivistystyönJotpaKoulutuksenOsasuorituksenSuoritus.className:
    case VapaanSivistystyönLukutaitokoulutuksenKokonaisuudenSuoritus.className:
    case VapaanSivistystyönVapaatavoitteisenKoulutuksenOsasuorituksenSuoritus.className:
    case VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenKieliopintojenSuoritus.className:
    case VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenOhjauksenSuoritus.className:
    case VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenTyöelämäJaYhteiskuntataitojenOpintojenSuoritus.className:
    case VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenValinnaistenOpintojenSuoritus.className:
    case VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenTyöelämäJaYhteiskuntataitojenTyöelämäJakso.className:
    case VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenValinnaistenOpintojenOsasuoritus.className:
      return true
    default:
      return false
  }
}
