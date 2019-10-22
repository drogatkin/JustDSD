package org.justcodecs.dsd;

public interface Filters {
	static double[] coefs_176 = { -0x094fe038b7bdfcp-088, -0x09a140d9658e16p-086, -0x0efb9eebd49340p-085,
			-0x09c2cc5209b41cp-083, -0x0af68aede100f0p-082, -0x0b0b0b2ad386a9p-081, -0x0a446365e1b49ap-080,
			-0x08fa22a9bc8695p-079, -0x0ef1f0dc7cd5efp-079, -0x0bf11805f896bfp-078, -0x093681ff4d53fep-077,
			-0x0dc87353b9ad97p-077, -0x0a0727cac81e5bp-076, -0x0e39ed51f5e17cp-076, -0x09db1ede0bbc97p-075,
			-0x0d5b0d57cca8bap-075, -0x08dba21a2b6dcbp-074, -0x0b8205ff91ee76p-074, -0x0ea5397aa648a3p-074,
			-0x09207642a1f455p-073, -0x0b21f8ad99c855p-073, -0x0d45ae2e55a365p-073, -0x0f6e63280d427ep-073,
			-0x08b7ee2c7e0245p-072, -0x098596af19d537p-072, -0x09f56202d2f3f3p-072, -0x09cf23c8a2c285p-072,
			-0x08cb3b96526556p-072, -0x0d2140214b6be4p-073, -0x0acca4d838b3b0p-074, +0x0d36ee959cea81p-074,
			+0x0c07bca8662c8bp-072, +0x0c12c08e830508p-071, +0x0a195da3b8ef0ap-070, +0x0f65d3035db580p-070,
			+0x0b12fb0b7e9b07p-069, +0x0f496510f69243p-069, +0x0a394782e27304p-068, +0x0d54bdc2fd5471p-068,
			+0x08818ba0d70263p-067, +0x0aa6f034d4e723p-067, +0x0d1d880f84766fp-067, +0x0fe5df97d7fca1p-067,
			+0x097eae8df0aeaep-066, +0x0b2ebccb841acep-066, +0x0cfd768c852b8ap-066, +0x0ee269f2b0c35bp-066,
			+0x0868f4af35fd3fp-065, +0x095e50887ea95fp-065, +0x0a479a70053331p-065, +0x0b18f597439a1ap-065,
			+0x0bc4357645ea7ep-065, +0x0c38c968a82643p-065, +0x0c63b828178e7ap-065, +0x0c2fae7533328dp-065,
			+0x0b852435913b46p-065, +0x0a4a9b255b5bf5p-065, +0x0864f9f13f2d4ep-065, +0x0b700c3feff5b3p-066,
			+0x089bfa805f565cp-067, -0x09aa9fa5505efep-067, -0x081864d110e8bep-065, -0x0efb9bd31364bfp-065,
			-0x0b953f0ba71c3bp-064, -0x082db3b7414787p-063, -0x0aeb2408764fd6p-063, -0x0e04273a374584p-063,
			-0x08bbe7028a86cep-062, -0x0aa15300f03d85p-062, -0x0caf2f0e2d595ap-062, -0x0ee0b795318be4p-062,
			-0x0897b2de00678bp-061, -0x09c9660ff8887bp-061, -0x0b003d0754bb8dp-061, -0x0c35f36186814bp-061,
			-0x0d63331157c188p-061, -0x0e7f9430f92a28p-061, -0x0f81a2e8f94122p-061, -0x082f75fafa8481p-060,
			-0x0886081c72f4fep-060, -0x08be6fcd17b8f7p-060, -0x08d23daf42d74cp-060, -0x08babfef99a6b7p-060,
			-0x08711ab310ce19p-060, -0x0fdcc7ba6f9506p-061, -0x0e5783b5ad26aap-061, -0x0c451a056319efp-061,
			-0x0998e609f45efdp-061, -0x0c8e6f295c5b7ap-062, -0x091678727431a8p-063, +0x09d330880a6a91p-063,
			+0x0fde318764e800p-062, +0x0e2e3f5392936fp-061, +0x0a9a6e4e0a55f3p-060, +0x0e817c7945ac6ep-060,
			+0x096575cbc501d5p-059, +0x0bb9f379f77916p-059, +0x0e3c0b181eac8fp-059, +0x0874658d85351bp-058,
			+0x09de3e48b6a944p-058, +0x0b595363360a10p-058, +0x0ce30b564270e3p-058, +0x0e78750bc72758p-058,
			+0x080b27ad9f5bcep-057, +0x08dc8929c4c701p-057, +0x09ae7d19c567d0p-057, +0x0a7f09f43c6277p-057,
			+0x0b4c256f472368p-057, +0x0c13bbf3bd7b21p-057, +0x0cd3b878eb78b5p-057, +0x0d8a0ca31dc95ep-057,
			+0x0e34b8fcd5f724p-057, +0x0ed1d520ec5fe1p-057, +0x0f5f97ab51c094p-057, +0x0fdc5dc6928781p-057,
			+0x08235916da3b33p-056, +0x084ea9be3ee6fcp-056, +0x086f9cd5cfaf87p-056, +0x0885cd4d424af6p-056,
			+0x0890f6c6cdc8dbp-056, +0x0890f6c6cdc8dbp-056, +0x0885cd4d424af6p-056, +0x086f9cd5cfaf88p-056,
			+0x084ea9be3ee6fcp-056, +0x08235916da3b33p-056, +0x0fdc5dc6928781p-057, +0x0f5f97ab51c093p-057,
			+0x0ed1d520ec5fe2p-057, +0x0e34b8fcd5f725p-057, +0x0d8a0ca31dc95fp-057, +0x0cd3b878eb78b5p-057,
			+0x0c13bbf3bd7b21p-057, +0x0b4c256f472368p-057, +0x0a7f09f43c6277p-057, +0x09ae7d19c567d1p-057,
			+0x08dc8929c4c702p-057, +0x080b27ad9f5bcep-057, +0x0e78750bc72759p-058, +0x0ce30b564270e4p-058,
			+0x0b595363360a11p-058, +0x09de3e48b6a944p-058, +0x0874658d85351bp-058, +0x0e3c0b181eac8fp-059,
			+0x0bb9f379f77918p-059, +0x096575cbc501d7p-059, +0x0e817c7945ac6fp-060, +0x0a9a6e4e0a55f2p-060,
			+0x0e2e3f53929370p-061, +0x0fde318764e806p-062, +0x09d330880a6a92p-063, -0x091678727431abp-063,
			-0x0c8e6f295c5b78p-062, -0x0998e609f45efep-061, -0x0c451a056319f4p-061, -0x0e5783b5ad26acp-061,
			-0x0fdcc7ba6f9507p-061, -0x08711ab310ce16p-060, -0x08babfef99a6b6p-060, -0x08d23daf42d74dp-060,
			-0x08be6fcd17b8fbp-060, -0x0886081c72f502p-060, -0x082f75fafa8480p-060, -0x0f81a2e8f94126p-061,
			-0x0e7f9430f92a30p-061, -0x0d63331157c189p-061, -0x0c35f36186814fp-061, -0x0b003d0754bb87p-061,
			-0x09c9660ff88879p-061, -0x0897b2de00678cp-061, -0x0ee0b795318beep-062, -0x0caf2f0e2d5967p-062,
			-0x0aa15300f03d87p-062, -0x08bbe7028a86d3p-062, -0x0e04273a37458dp-063, -0x0aeb2408764fd7p-063,
			-0x082db3b741478ap-063, -0x0b953f0ba71c34p-064, -0x0efb9bd31364bcp-065, -0x081864d110e8c0p-065,
			-0x09aa9fa5505ef5p-067, +0x089bfa805f5666p-067, +0x0b700c3feff5b3p-066, +0x0864f9f13f2d52p-065,
			+0x0a4a9b255b5bf9p-065, +0x0b852435913b4ep-065, +0x0c2fae75333298p-065, +0x0c63b828178e75p-065,
			+0x0c38c968a82644p-065, +0x0bc4357645ea6bp-065, +0x0b18f597439a11p-065, +0x0a479a70053329p-065,
			+0x095e50887ea96ap-065, +0x0868f4af35fd47p-065, +0x0ee269f2b0c366p-066, +0x0cfd768c852b91p-066,
			+0x0b2ebccb841ad9p-066, +0x097eae8df0aeafp-066, +0x0fe5df97d7fc8ep-067, +0x0d1d880f847665p-067,
			+0x0aa6f034d4e724p-067, +0x08818ba0d70269p-067, +0x0d54bdc2fd545bp-068, +0x0a394782e2730cp-068,
			+0x0f496510f69221p-069, +0x0b12fb0b7e9b2ep-069, +0x0f65d3035db58ap-070, +0x0a195da3b8ef0cp-070,
			+0x0c12c08e830526p-071, +0x0c07bca8662bcap-072, +0x0d36ee959ceb27p-074, -0x0acca4d838b2e4p-074,
			-0x0d2140214b6b49p-073, -0x08cb3b96526663p-072, -0x09cf23c8a2c2cap-072, -0x09f56202d2ef20p-072,
			-0x098596af19d34ap-072, -0x08b7ee2c7e0513p-072, -0x0f6e63280d3af9p-073, -0x0d45ae2e55a2f3p-073,
			-0x0b21f8ad99d6d2p-073, -0x09207642a1f80dp-073, -0x0ea5397aa651cap-074, -0x0b8205ff9212b9p-074,
			-0x08dba21a2b4d59p-074, -0x0d5b0d57cc9f29p-075, -0x09db1ede0b5037p-075, -0x0e39ed51f58522p-076,
			-0x0a0727cac82524p-076, -0x0dc87353ba031fp-077, -0x093681ff4d2b4bp-077, -0x0bf11805f8cfbcp-078,
			-0x0ef1f0dc7d3d4dp-079, -0x08fa22a9bc984dp-079, -0x0a446365dfc6efp-080, -0x0b0b0b2ad257fdp-081,
			-0x0af68aedde3506p-082, -0x09c2cc52051fc3p-083, -0x0efb9eebe9a976p-085, -0x09a140d95e96edp-086,
			-0x094fe038b7bdfcp-088, };

	static int tzero_176 = 120;

	static double[] coefs_352 = { +3.130441005359395694e-08, +1.244182214744588123e-07, +3.423230509967408957e-07,
			+7.410039764949090706e-07, +1.319400334374194979e-06, +1.930520892991081870e-06, +2.166655190537391817e-06,
			+1.249721855219005082e-06, -2.017460145032201133e-06, -9.163058931391722015e-06, -2.174079575545870077e-05,
			-4.074928958725350180e-05, -6.577634378272832012e-05, -9.396247155265073355e-05, -1.189970287491284975e-04,
			-1.304796361231894922e-04, -1.140625650874684021e-04, -5.279407053811266003e-05, +7.002968738383527972e-05,
			+2.664463454252759917e-04, +5.381636200535649473e-04, +8.704322683580221955e-04, +1.226696225277854957e-03,
			+1.545601648013235048e-03, +1.742046839472948102e-03, +1.713709169690936975e-03, +1.353838005269448076e-03,
			+5.700762133516592374e-04, -6.922187080790708326e-04, -2.425035959059578146e-03, -4.535184322001496043e-03,
			-6.828859761015334574e-03, -9.007905078766049317e-03, -1.068138779745870029e-02, -1.139427235000863015e-02,
			-1.067241812471033009e-02, -8.080250212687496714e-03, -3.284703416210725969e-03, +3.883043418804416145e-03,
			+1.337747462658970057e-02, +2.490921351762261093e-02, +3.794294849101870204e-02, +5.172629311427257015e-02,
			+6.534876523171298524e-02, +7.782552527068174741e-02, +8.819647126516944047e-02, +9.562845727714668065e-02,
			+9.950731974056657714e-02, +9.950731974056657714e-02, +9.562845727714668065e-02, +8.819647126516944047e-02,
			+7.782552527068174741e-02, +6.534876523171298524e-02, +5.172629311427257015e-02, +3.794294849101870204e-02,
			+2.490921351762261093e-02, +1.337747462658970057e-02, +3.883043418804416145e-03, -3.284703416210725969e-03,
			-8.080250212687496714e-03, -1.067241812471033009e-02, -1.139427235000863015e-02, -1.068138779745870029e-02,
			-9.007905078766049317e-03, -6.828859761015334574e-03, -4.535184322001496043e-03, -2.425035959059578146e-03,
			-6.922187080790708326e-04, +5.700762133516592374e-04, +1.353838005269448076e-03, +1.713709169690936975e-03,
			+1.742046839472948102e-03, +1.545601648013235048e-03, +1.226696225277854957e-03, +8.704322683580221955e-04,
			+5.381636200535649473e-04, +2.664463454252759917e-04, +7.002968738383527972e-05, -5.279407053811266003e-05,
			-1.140625650874684021e-04, -1.304796361231894922e-04, -1.189970287491284975e-04, -9.396247155265073355e-05,
			-6.577634378272832012e-05, -4.074928958725350180e-05, -2.174079575545870077e-05, -9.163058931391722015e-06,
			-2.017460145032201133e-06, +1.249721855219005082e-06, +2.166655190537391817e-06, +1.930520892991081870e-06,
			+1.319400334374194979e-06, +7.410039764949090706e-07, +3.423230509967408957e-07, +1.244182214744588123e-07,
			+3.130441005359395694e-08, };

	static int tzero_352 = 48;

	static double[] coefs_88 = { +0x088c3e7affed0dp-085, +0x08d84c484a16a0p-085, +0x0a1b25e846d8cap-085,
			+0x0c3ffcf7b37f09p-085, +0x0f2ff26ec11256p-085, +0x09680418a79253p-084, +0x0b7f4f5a892b27p-084,
			+0x0dc840cf4dc7e9p-084, +0x081361c9cf6402p-083, +0x093b31db259e54p-083, +0x0a441ca13ef246p-083,
			+0x0b105398bede28p-083, +0x0b7a7a0e5c2d0ep-083, +0x0b54620eec4b78p-083, +0x0a65be8f0f8da3p-083,
			+0x086ad074e5ff44p-083, +0x0a262aac3877aap-084, -0x0900e78e606f35p-130, -0x0e787788df48d9p-084,
			-0x088fdfa82d1d21p-082, -0x0f1cf65eef1349p-082, -0x0bcb0687fad775p-081, -0x08953f9c8415aap-080,
			-0x0bef378e0575ecp-080, -0x0807399a005cd4p-079, -0x0a881177a82c29p-079, -0x0d89b904a83730p-079,
			-0x088e510ad8c367p-078, -0x0aa8f6b7361328p-078, -0x0d1d980b678a2bp-078, -0x0ff514ed311918p-078,
			-0x099c1ae2ff7001p-077, -0x0b77baedeccefbp-077, -0x0d9162211635e5p-077, -0x0fec9d69e42c0ap-077,
			-0x094632af6a726ap-076, -0x0ab9778f64f988p-076, -0x0c50bbd64fff86p-076, -0x0e0c04deb43892p-076,
			-0x0fea9f036599a8p-076, -0x08f57d8d6894e6p-075, -0x0a0544b4aecdc5p-075, -0x0b22c91f37a06ap-075,
			-0x0c4b8640e8af4cp-075, -0x0d7c394585b55cp-075, -0x0eb0cad8aa256fp-075, -0x0fe4388ca96a50p-075,
			-0x08883f175b2737p-074, -0x09173fb17798c3p-074, -0x099af8f7bb07dep-074, -0x0a0ea4953796ebp-074,
			-0x0a6cd185b6f94bp-074, -0x0aaf5bb6a74cf6p-074, -0x0acf650cc105aep-074, -0x0ac55020981077p-074,
			-0x0a88bd0980be68p-074, -0x0a108892d20758p-074, -0x0952ce3b6f2a35p-074, -0x0844ed6281aa52p-074,
			-0x0db7240c7ef575p-075, -0x0a1582f4b3ed2ep-075, -0x0b17adec5fc6edp-076, +0x0f05f9709f56ccp-123,
			+0x0d51d74b39286ap-076, +0x0e8a5e8610e3fep-075, +0x0be003cc6b7026p-074, +0x08995e6d2376dap-073,
			+0x0ba57198dd7b39p-073, +0x0f1ac2ef059717p-073, +0x097fbea032a421p-072, +0x0baca5be9923bap-072,
			+0x0e16991d2c6bfep-072, +0x085fd97e1a45edp-071, +0x09d4c7926dbb45p-071, +0x0b6a9980d416b7p-071,
			+0x0d217a2ce5089ep-071, +0x0ef92f6a55aaf9p-071, +0x0878861cf14ee5p-070, +0x0983f14e65ab64p-070,
			+0x0a9dfa8f74a277p-070, +0x0bc5741e126147p-070, +0x0cf8d9e568c6cap-070, +0x0e364a4ca37053p-070,
			+0x0f7b7f38e2b58ep-070, +0x0862e3af17a502p-069, +0x0908fffec0c5f1p-069, +0x09ae479804084cp-069,
			+0x0a50af70e3768ap-069, +0x0aedec4fb107cdp-069, +0x0b83712e2f0ef1p-069, +0x0c0e6e2c28ddf1p-069,
			+0x0c8bd0252cefdfp-069, +0x0cf840fd4de1d1p-069, +0x0d5028b8d5b7acp-069, +0x0d8faf72a1eac5p-069,
			+0x0db2c04469a8d7p-069, +0x0db50d33758446p-069, +0x0d9214334c7275p-069, +0x0d45254e89459bp-069,
			+0x0cc96a046a95e3p-069, +0x0c19ede7ba8ffap-069, +0x0b31a889626f91p-069, +0x0a0b88b65d68a5p-069,
			+0x08a2810dc8303ap-069, +0x0de32be0fbff29p-070, +0x09e7d99027303bp-070, +0x0a93727c3dcf6ap-071,
			-0x0b20911afa2f7ap-118, -0x0bfadfcdd4abd7p-071, -0x0cb5de8e6e24a0p-070, -0x0a17c9edbc465ep-069,
			-0x0e37d58b493f8bp-069, -0x095e793b4e1e73p-068, -0x0bd41e0bd42f84p-068, -0x0e7cf8693f3027p-068,
			-0x08ac566c7f3317p-067, -0x0a332c82d32e71p-067, -0x0bd243d61ea4a2p-067, -0x0d88926364ced6p-067,
			-0x0f54b90c20b9efp-067, -0x089a7f100e32b1p-066, -0x0993a41fadc966p-066, -0x0a948cd49a4f3ep-066,
			-0x0b9bc60739d0fep-066, -0x0ca7a67e7e86fap-066, -0x0db64d7de7ff4cp-066, -0x0ec5a1b74091c1p-066,
			-0x0fd350aba45628p-066, -0x086e6743a100bdp-065, -0x08efab4184d1f6p-065, -0x096bf5eb8fbdeep-065,
			-0x09e1ada4342cc2p-065, -0x0a4f1ead7010a4p-065, -0x0ab27c98d061eap-065, -0x0b09e41247eaf7p-065,
			-0x0b535d0949337ep-065, -0x0b8cdd3af4e7b3p-065, -0x0bb44b1f7ca5cfp-065, -0x0bc7813c16104fp-065,
			-0x0bc451da03fc44p-065, -0x0ba88b2254602fp-065, -0x0b71fb9cfa5eaap-065, -0x0b1e7710e5ad7ep-065,
			-0x0aabdbc1a53672p-065, -0x0a18180603e1f0p-065, -0x09613031e600c6p-065, -0x088544cc7df3b2p-065,
			-0x0f05321573d0b5p-066, -0x0caf33092ace9cp-066, -0x0a05c635859371p-066, -0x0e0d2803b5ecacp-067,
			-0x0ebf06fd4a2155p-068, +0x0b7083cbdf43bap-115, +0x0812ce3a1a5538p-067, +0x086c81cb18e415p-066,
			+0x0d28a8bc9d9a0dp-066, +0x091e38e0d5e010p-065, +0x0bd2d17e95c241p-065, +0x0eb0936bd38c78p-065,
			+0x08dac035d04f3fp-064, +0x0a6f90e14bd486p-064, +0x0c1541e25c86adp-064, +0x0dca190965b6b0p-064,
			+0x0f8c1a4dbfa99ep-064, +0x08ac835d0fd7e4p-063, +0x09972de743e06cp-063, +0x0a84a9b088ede8p-063,
			+0x0b7372033a20e2p-063, +0x0c61e10544f40fp-063, +0x0d4e30803f067fp-063, +0x0e367af8b95951p-063,
			+0x0f18bd17cc22c8p-063, +0x0ff2d76958aca7p-063, +0x086148387a456cp-062, +0x08c2cb8b6dace1p-062,
			+0x091cc2b6d1689fp-062, +0x096df1e86091b0p-062, +0x09b5162da0242dp-062, +0x09f0e75a9d8be1p-062,
			+0x0a201a1afe0d3bp-062, +0x0a41622be59550p-062, +0x0a5374bcd3db58p-062, +0x0a550af52dad30p-062,
			+0x0a44e49bbe95a4p-062, +0x0a21cadd15a139p-062, +0x09ea932d37c8fap-062, +0x099e2240bc680dp-062,
			+0x093b6f19050e1fp-062, +0x08c1861ee2fa05p-062, +0x082f8c46a285c2p-062, +0x0f098470419c25p-063,
			+0x0d810eea85b0f9p-063, +0x0bc4baf1c0c5a7p-063, +0x09d3d58ff08d62p-063, +0x0f5bf7bb475ef2p-064,
			+0x0aa6418e1abf35p-064, +0x0b0e4a3a9e0f70p-065, -0x0b5066436c282ap-113, -0x0bda0aeae7d853p-065,
			-0x0c3cef5b819ff5p-064, -0x0975f3288613ebp-063, -0x0cfa984f221101p-063, -0x0854cb1e6f5c1bp-062,
			-0x0a3fd2e1e94946p-062, -0x0c3c83ea6ff870p-062, -0x0e48c24d576329p-062, -0x08311ad45768bep-061,
			-0x094324b8ec26e6p-061, -0x0a5916d55d25b1p-061, -0x0b716c0068e908p-061, -0x0c8a82ac654b73p-061,
			-0x0da29de44b2ba0p-061, -0x0eb7e680a347c8p-061, -0x0fc86c94962af0p-061, -0x086914897ebccbp-060,
			-0x08e97fd5770686p-060, -0x0964606d646e44p-060, -0x09d8961c3ab70cp-060, -0x0a44f9767e1d9ap-060,
			-0x0aa85d30a575ffp-060, -0x0b018f8cc0312ap-060, -0x0b4f5bddda0fe4p-060, -0x0b908c1f623f62p-060,
			-0x0bc3ea9ea93137p-060, -0x0be843b4582527p-060, -0x0bfc678b977f74p-060, -0x0bff2bf46e1675p-060,
			-0x0bef6e3ebc081bp-060, -0x0bcc151b0fcba3p-060, -0x0b94127e747357p-060, -0x0b4665863cca44p-060,
			-0x0ae21c58b87ecdp-060, -0x0a6655ffaf25e1p-060, -0x09d2443970db69p-060, -0x09252d3d45d01ep-060,
			-0x085e6d7005672fp-060, -0x0efaf20b4582d3p-061, -0x0d03bb190fa564p-061, -0x0ad686bf52de55p-061,
			-0x0872ddf8ac4256p-061, -0x0bb108e37c140cp-062, -0x0c1deacaea2d84p-063, +0x0c62232438f207p-112,
			+0x0cf5b04c173345p-063, +0x0d6010c630f6d4p-062, +0x0a56d1526ed6d6p-061, +0x0e3080377d6557p-061,
			+0x091dc5941788cdp-060, +0x0b3b1291f83c12p-060, +0x0d6f1f4f2e5146p-060, +0x0fb8c335e0153cp-060,
			+0x090b5ac4023ba2p-059, +0x0a43c71c0572dap-059, +0x0b84e3792d14fep-059, +0x0ccdde2b7cdea4p-059,
			+0x0e1dd7b132af82p-059, +0x0f73e3821bbd01p-059, +0x08678478812e9ap-058, +0x091722111cb04bp-058,
			+0x09c8438b116f5fp-058, +0x0a7a5d63355fddp-058, +0x0b2ce02a44baa7p-058, +0x0bdf392400c6d3p-058,
			+0x0c90d2ed55a9ffp-058, +0x0d411628a26a4cp-058, +0x0def6a2f3147a8p-058, +0x0e9b35c6e313bdp-058,
			+0x0f43dfdb0652abp-058, +0x0fe8d0374ac8bcp-058, +0x0844b821dddf74p-057, +0x089295e053647cp-057,
			+0x08ddb8c0a920c2p-057, +0x0925da12330525p-057, +0x096ab564753868p-057, +0x09ac08df622fc4p-057,
			+0x09e99598cda12dp-057, +0x0a231fe68e1905p-057, +0x0a586faccb85d5p-057, +0x0a8950a7fe808fp-057,
			+0x0ab592b22a6e30p-057, +0x0add0a02e3c55fp-057, +0x0aff8f69bbbba1p-057, +0x0b1d0082b353d9p-057,
			+0x0b353fe4612b63p-057, +0x0b4835477f6329p-057, +0x0b55cda7a18cafp-057, +0x0b5dfb5cdd848cp-057,
			+0x0b60b62e3d87e7p-057, +0x0b5dfb5cdd848cp-057, +0x0b55cda7a18cafp-057, +0x0b4835477f6329p-057,
			+0x0b353fe4612b63p-057, +0x0b1d0082b353d8p-057, +0x0aff8f69bbbba1p-057, +0x0add0a02e3c560p-057,
			+0x0ab592b22a6e30p-057, +0x0a8950a7fe808fp-057, +0x0a586faccb85d5p-057, +0x0a231fe68e1906p-057,
			+0x09e99598cda12dp-057, +0x09ac08df622fc3p-057, +0x096ab564753868p-057, +0x0925da12330525p-057,
			+0x08ddb8c0a920c2p-057, +0x089295e053647dp-057, +0x0844b821dddf74p-057, +0x0fe8d0374ac8bdp-058,
			+0x0f43dfdb0652aap-058, +0x0e9b35c6e313bdp-058, +0x0def6a2f3147a8p-058, +0x0d411628a26a4dp-058,
			+0x0c90d2ed55a9ffp-058, +0x0bdf392400c6d3p-058, +0x0b2ce02a44baa8p-058, +0x0a7a5d63355fdep-058,
			+0x09c8438b116f5fp-058, +0x091722111cb04ap-058, +0x08678478812e9ap-058, +0x0f73e3821bbd02p-059,
			+0x0e1dd7b132af83p-059, +0x0ccdde2b7cdea5p-059, +0x0b84e3792d1500p-059, +0x0a43c71c0572dap-059,
			+0x090b5ac4023ba2p-059, +0x0fb8c335e0153ep-060, +0x0d6f1f4f2e5147p-060, +0x0b3b1291f83c12p-060,
			+0x091dc5941788cep-060, +0x0e3080377d6557p-061, +0x0a56d1526ed6d7p-061, +0x0d6010c630f6d3p-062,
			+0x0cf5b04c173346p-063, +0x0c62232438f208p-112, -0x0c1deacaea2d85p-063, -0x0bb108e37c140cp-062,
			-0x0872ddf8ac4257p-061, -0x0ad686bf52de57p-061, -0x0d03bb190fa566p-061, -0x0efaf20b4582d2p-061,
			-0x085e6d7005672fp-060, -0x09252d3d45d01fp-060, -0x09d2443970db6ap-060, -0x0a6655ffaf25e3p-060,
			-0x0ae21c58b87ecfp-060, -0x0b4665863cca48p-060, -0x0b94127e747358p-060, -0x0bcc151b0fcba3p-060,
			-0x0bef6e3ebc081ap-060, -0x0bff2bf46e1676p-060, -0x0bfc678b977f75p-060, -0x0be843b4582527p-060,
			-0x0bc3ea9ea9313ap-060, -0x0b908c1f623f65p-060, -0x0b4f5bddda0fe2p-060, -0x0b018f8cc03129p-060,
			-0x0aa85d30a57601p-060, -0x0a44f9767e1d9bp-060, -0x09d8961c3ab70dp-060, -0x0964606d646e46p-060,
			-0x08e97fd5770689p-060, -0x086914897ebccfp-060, -0x0fc86c94962af2p-061, -0x0eb7e680a347c6p-061,
			-0x0da29de44b2ba1p-061, -0x0c8a82ac654b75p-061, -0x0b716c0068e90ap-061, -0x0a5916d55d25b5p-061,
			-0x094324b8ec26e9p-061, -0x08311ad45768bfp-061, -0x0e48c24d57632ap-062, -0x0c3c83ea6ff870p-062,
			-0x0a3fd2e1e94947p-062, -0x0854cb1e6f5c19p-062, -0x0cfa984f221101p-063, -0x0975f3288613ecp-063,
			-0x0c3cef5b819ff7p-064, -0x0bda0aeae7d85ap-065, -0x0b5066436c2829p-113, +0x0b0e4a3a9e0f6fp-065,
			+0x0aa6418e1abf39p-064, +0x0f5bf7bb475ef5p-064, +0x09d3d58ff08d64p-063, +0x0bc4baf1c0c5a6p-063,
			+0x0d810eea85b0ffp-063, +0x0f098470419c22p-063, +0x082f8c46a285bfp-062, +0x08c1861ee2fa09p-062,
			+0x093b6f19050e20p-062, +0x099e2240bc680bp-062, +0x09ea932d37c8fep-062, +0x0a21cadd15a13dp-062,
			+0x0a44e49bbe95a9p-062, +0x0a550af52dad32p-062, +0x0a5374bcd3db58p-062, +0x0a41622be59552p-062,
			+0x0a201a1afe0d39p-062, +0x09f0e75a9d8be5p-062, +0x09b5162da0242fp-062, +0x096df1e86091b2p-062,
			+0x091cc2b6d168a1p-062, +0x08c2cb8b6dace2p-062, +0x086148387a456ap-062, +0x0ff2d76958aca1p-063,
			+0x0f18bd17cc22cep-063, +0x0e367af8b95954p-063, +0x0d4e30803f067fp-063, +0x0c61e10544f41ap-063,
			+0x0b7372033a20e9p-063, +0x0a84a9b088ede3p-063, +0x09972de743e06fp-063, +0x08ac835d0fd7e7p-063,
			+0x0f8c1a4dbfa99cp-064, +0x0dca190965b6abp-064, +0x0c1541e25c86b1p-064, +0x0a6f90e14bd48fp-064,
			+0x08dac035d04f3cp-064, +0x0eb0936bd38c78p-065, +0x0bd2d17e95c245p-065, +0x091e38e0d5e00fp-065,
			+0x0d28a8bc9d9a18p-066, +0x086c81cb18e418p-066, +0x0812ce3a1a5539p-067, +0x0b7083cbdf43b7p-115,
			-0x0ebf06fd4a2159p-068, -0x0e0d2803b5ecacp-067, -0x0a05c63585936bp-066, -0x0caf33092acea6p-066,
			-0x0f05321573d0b9p-066, -0x088544cc7df3b0p-065, -0x09613031e600c4p-065, -0x0a18180603e1f9p-065,
			-0x0aabdbc1a5366dp-065, -0x0b1e7710e5ad8ap-065, -0x0b71fb9cfa5eb2p-065, -0x0ba88b22546033p-065,
			-0x0bc451da03fc41p-065, -0x0bc7813c161059p-065, -0x0bb44b1f7ca5d4p-065, -0x0b8cdd3af4e7b6p-065,
			-0x0b535d0949338ap-065, -0x0b09e41247eb00p-065, -0x0ab27c98d061ebp-065, -0x0a4f1ead701099p-065,
			-0x09e1ada4342ccap-065, -0x096bf5eb8fbdf0p-065, -0x08efab4184d1f5p-065, -0x086e6743a100c7p-065,
			-0x0fd350aba45622p-066, -0x0ec5a1b74091bbp-066, -0x0db64d7de7ff63p-066, -0x0ca7a67e7e8708p-066,
			-0x0b9bc60739d100p-066, -0x0a948cd49a4f3dp-066, -0x0993a41fadc976p-066, -0x089a7f100e32aep-066,
			-0x0f54b90c20b9e1p-067, -0x0d88926364ced8p-067, -0x0bd243d61ea4aap-067, -0x0a332c82d32e6ap-067,
			-0x08ac566c7f3329p-067, -0x0e7cf8693f3038p-068, -0x0bd41e0bd42f89p-068, -0x095e793b4e1e76p-068,
			-0x0e37d58b493f88p-069, -0x0a17c9edbc4654p-069, -0x0cb5de8e6e2492p-070, -0x0bfadfcdd4abf0p-071,
			-0x0b20911afa2f8bp-118, +0x0a93727c3dcf71p-071, +0x09e7d99027303dp-070, +0x0de32be0fbff2fp-070,
			+0x08a2810dc83037p-069, +0x0a0b88b65d6899p-069, +0x0b31a889626f9ep-069, +0x0c19ede7ba8ffep-069,
			+0x0cc96a046a95e8p-069, +0x0d45254e8945afp-069, +0x0d9214334c7284p-069, +0x0db50d3375844fp-069,
			+0x0db2c04469a8c5p-069, +0x0d8faf72a1eae7p-069, +0x0d5028b8d5b7acp-069, +0x0cf840fd4de1dbp-069,
			+0x0c8bd0252cefd8p-069, +0x0c0e6e2c28ddf6p-069, +0x0b83712e2f0ef0p-069, +0x0aedec4fb107e2p-069,
			+0x0a50af70e37683p-069, +0x09ae4798040838p-069, +0x0908fffec0c5ecp-069, +0x0862e3af17a4c6p-069,
			+0x0f7b7f38e2b556p-070, +0x0e364a4ca37089p-070, +0x0cf8d9e568c6f0p-070, +0x0bc5741e1261e0p-070,
			+0x0a9dfa8f74a2aap-070, +0x0983f14e65ab13p-070, +0x0878861cf14e34p-070, +0x0ef92f6a55a998p-071,
			+0x0d217a2ce508f9p-071, +0x0b6a9980d415bfp-071, +0x09d4c7926dbbc9p-071, +0x085fd97e1a4755p-071,
			+0x0e16991d2c6cc3p-072, +0x0baca5be99249ap-072, +0x097fbea032a382p-072, +0x0f1ac2ef0593d8p-073,
			+0x0ba57198dd78dep-073, +0x08995e6d2375ffp-073, +0x0be003cc6b707ap-074, +0x0e8a5e8610e6fep-075,
			+0x0d51d74b392b74p-076, +0x0f05f9709f5cd9p-123, -0x0b17adec5fc8adp-076, -0x0a1582f4b3e9eep-075,
			-0x0db7240c7ef560p-075, -0x0844ed6281ab33p-074, -0x0952ce3b6f2c63p-074, -0x0a108892d20bbap-074,
			-0x0a88bd0980bfe3p-074, -0x0ac5502098162dp-074, -0x0acf650cc10c76p-074, -0x0aaf5bb6a75482p-074,
			-0x0a6cd185b6f116p-074, -0x0a0ea495379ad5p-074, -0x099af8f7bb11d8p-074, -0x09173fb1779631p-074,
			-0x08883f175b2f2fp-074, -0x0fe4388ca97c9bp-075, -0x0eb0cad8aa4327p-075, -0x0d7c394585c420p-075,
			-0x0c4b8640e8cd1ap-075, -0x0b22c91f3790f4p-075, -0x0a0544b4aecaaep-075, -0x08f57d8d6897c8p-075,
			-0x0fea9f0365ad05p-076, -0x0e0c04deb41f26p-076, -0x0c50bbd6500855p-076, -0x0ab9778f65201ep-076,
			-0x094632af6aadf4p-076, -0x0fec9d69e422aap-077, -0x0d916221166854p-077, -0x0b77baeded0f95p-077,
			-0x099c1ae2ff4a5ep-077, -0x0ff514ed3168aep-078, -0x0d1d980b67d696p-078, -0x0aa8f6b73642a1p-078,
			-0x088e510ad8ccb4p-078, -0x0d89b904a877e5p-079, -0x0a881177a83b97p-079, -0x0807399a0061bdp-079,
			-0x0bef378e058404p-080, -0x08953f9c84c3d4p-080, -0x0bcb0687fb66fcp-081, -0x0f1cf65eef9c84p-082,
			-0x088fdfa82d74d7p-082, -0x0e787788df3438p-084, -0x0900e78e5fb961p-130, +0x0a262aac392d6bp-084,
			+0x086ad074e717efp-083, +0x0a65be8f110998p-083, +0x0b54620eeeb35dp-083, +0x0b7a7a0e5dfa75p-083,
			+0x0b105398be1674p-083, +0x0a441ca13ef246p-083, +0x093b31db25d4dap-083, +0x081361c9ca6bf2p-083,
			+0x0dc840cf4cc5aap-084, +0x0b7f4f5a8c0277p-084, +0x09680418ae1dd7p-084, +0x0f2ff26ee95635p-085,
			+0x0c3ffcf7c39d30p-085, +0x0a1b25e847e632p-085, +0x08d84c48491064p-085, +0x088c3e7affed0dp-085, };

	static int tzero_88 = 288;

}
